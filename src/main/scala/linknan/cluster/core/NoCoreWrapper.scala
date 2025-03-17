package linknan.cluster.core

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import coupledL2.L2ParamKey
import coupledL2.tl2chi.TL2CHICoupledL2
import freechips.rocketchip.diplomacy.{IdRange, TransferSizes}
import freechips.rocketchip.tilelink.{BankBinder, TLBuffer, TLClientNode, TLMasterParameters, TLMasterPortParameters, TLXbar}
import linknan.cluster.{BlockTestIO, BlockTestIOParams}
import linknan.utils.{connectByName, connectChiChn}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.bundlebridge.BundleBridgeSource
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import xiangshan.{HasXSParameter, XSCoreParamsKey}
import xijiang.Node
import xs.utils.common.{AliasField, IsKeywordField, PrefetchField, PrefetchRecv, VaddrField}
import xs.utils.tl.ReqSourceField
import zhujiang.chi.{DataFlit, RReqFlit, RespFlit, SnoopFlit}

class NoCoreWrapper (node:Node)(implicit p:Parameters) extends BaseCoreWrapper with HasXSParameter {
  private val coreP = p(XSCoreParamsKey)
  private val dcacheP = coreP.dcacheParametersOpt.get
  private val icacheP = coreP.icacheParameters

  private val dcacheNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "dcache",
      sourceId = IdRange(0, dcacheP.nMissEntries + dcacheP.nReleaseEntries + 1),
      supportsProbe = TransferSizes(dcacheP.blockBytes)
    )),
    requestFields = Seq(PrefetchField(), ReqSourceField(), VaddrField(VAddrBits - log2Up(dcacheP.blockBytes))) ++ dcacheP.aliasBitsOpt.map(AliasField),
    echoFields = Seq(IsKeywordField())
  )))

  private val ptwNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "ptw",
      sourceId = IdRange(0, coreP.l2tlbParameters.llptwsize + 1 + 1)
    )),
    requestFields = Seq(ReqSourceField())
  )))

  private val icacheNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    Seq(TLMasterParameters.v1(
      name = "icache",
      sourceId = IdRange(0, icacheP.nFetchMshr + icacheP.nPrefetchMshr + 1),
    )),
    requestFields = icacheP.reqFields,
    echoFields = icacheP.echoFields
  )))

  private val mmioSourceBits = log2Ceil(icacheP.nMMIOs.max(coreP.UncacheBufferSize)) + 1
  private val cioNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "uncache",
      sourceId = IdRange(0, 1 << mmioSourceBits)
    ))
  )))

  private val preftchNode = coreP.prefetcher.map(_ => BundleBridgeSource(() => new PrefetchRecv))

  private val cacheXBar = LazyModule(new TLXbar)

  cacheXBar.node :=* TLBuffer.chainNode(1, Some(s"l1d_buffer")) :=* dcacheNode
  cacheXBar.node :=* TLBuffer.chainNode(1, Some(s"ptw_buffer")) :=* ptwNode
  cacheXBar.node :=* TLBuffer.chainNode(1, Some(s"l1i_buffer")) :=* icacheNode

  //L2 Connections
  private val l2cache = LazyModule(new TL2CHICoupledL2)
  private val tpMetaSinkNode = l2cache.tpmeta_source_node.map(_.makeSink())
  private val tpMetaSourceNode = l2cache.tpmeta_sink_node.map(n => BundleBridgeSource(n.genOpt.get))
  l2cache.tpmeta_sink_node.foreach(_ := tpMetaSourceNode.get)

  l2cache.pf_recv_node.foreach(_ := preftchNode.get)
  l2cache.mmioNode.get :*= TLBuffer() :*= cioNode
  l2cache.managerNode :=*
    TLBuffer.chainNode(1, Some(s"l2_bank_buffer")) :=*
    TLXbar() :=*
    BankBinder(p(XSCoreParamsKey).L2NBanks, p(L2ParamKey).blockBytes) :*=
    l2cache.node :*=
    TLBuffer() :*=
    cacheXBar.node

  lazy val module = new NoCoreWrapperImpl
  val btioParams = BlockTestIOParams(
    cioTlParams = cioNode.edges.out.head.bundle,
    icacheTlParams = icacheNode.edges.out.head.bundle,
    ptwTlParams = ptwNode.edges.out.head.bundle,
    dcacheTlParams = dcacheNode.edges.out.head.bundle,
    node = node
  )
  @instantiable
  class NoCoreWrapperImpl extends BaseCoreWrapperImpl(this, node) {
    @public
    val btio = IO(new BlockTestIO(btioParams))
    btio.cio <> cioNode.out.head._1
    btio.icache <> icacheNode.out.head._1
    btio.ptw <> ptwNode.out.head._1
    btio.dcache <> dcacheNode.out.head._1
    btio.mhartid := io.mhartid
    btio.clock := io.clock
    btio.reset := io.reset

    l2cache.module.io.hartId := io.mhartid
    l2cache.module.io_nodeID := 0.U(7.W)
    l2cache.module.io.pfCtrlFromCore := DontCare
    l2cache.module.io.l2_tlb_req := DontCare
    l2cache.module.io.debugTopDown := DontCare
    l2cache.module.io.dft.get := io.dft.func
    l2cache.module.io.dft_reset.get := io.dft.reset
    l2cache.module.io.l2Flush.foreach(_ := false.B)

    tpMetaSinkNode.foreach(_.in.head._1.ready := false.B)
    tpMetaSourceNode.foreach(_.out.head._1 := DontCare)

    cpuHalt := false.B

    private val txReqFlit = Wire(Decoupled(new RReqFlit))
    connectByName(txReqFlit, l2cache.module.io_chi.tx.req)
    txReqFlit.bits.SnoopMe := l2cache.module.io_chi.tx.req.bits.snoopMe
    connectChiChn(pdc.io.icn.rx.req.get, txReqFlit)

    private val txRspFlit = Wire(Decoupled(new RespFlit))
    connectByName(txRspFlit, l2cache.module.io_chi.tx.rsp)
    connectChiChn(pdc.io.icn.rx.resp.get, txRspFlit)

    private val txDatFlit = Wire(Decoupled(new DataFlit))
    connectByName(txDatFlit, l2cache.module.io_chi.tx.dat)
    connectChiChn(pdc.io.icn.rx.data.get, txDatFlit)

    private val rxSnpFlit = Wire(Decoupled(new SnoopFlit))
    connectByName(l2cache.module.io_chi.rx.snp, rxSnpFlit)
    connectChiChn(rxSnpFlit, pdc.io.icn.tx.snoop.get)

    private val rxRspFlit = Wire(Decoupled(new RespFlit))
    connectByName(l2cache.module.io_chi.rx.rsp, rxRspFlit)
    connectChiChn(rxRspFlit, pdc.io.icn.tx.resp.get)

    private val rxDatFlit = Wire(Decoupled(new DataFlit))
    connectByName(l2cache.module.io_chi.rx.dat, rxDatFlit)
    connectChiChn(rxDatFlit, pdc.io.icn.tx.data.get)
  }
}
