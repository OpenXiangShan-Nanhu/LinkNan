package linknan.cluster.core

import chisel3._
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import coupledL2.tl2chi.TL2CHICoupledL2
import freechips.rocketchip.diplomacy.{IdRange, TransferSizes}
import freechips.rocketchip.tilelink.{BankBinder, TLBuffer, TLClientNode, TLMasterParameters, TLMasterPortParameters, TLXbar}
import linknan.cluster.{BlockTestIO, BlockTestIOParams}
import linknan.utils.{connectByName, connectChiChn}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.bundlebridge.BundleBridgeSource
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import xiangshan.cache.DCache
import xiangshan.{HasXSParameter, XSCoreParamsKey}
import xijiang.Node
import xs.utils.cache.common.{AliasField, IsKeywordField, L2ParamKey, PrefetchField, PrefetchRecv, VaddrField}
import xs.utils.debug.{HardwareAssertion, HardwareAssertionKey}
import xs.utils.tl.ReqSourceField
import zhujiang.HasZJParams
import zhujiang.chi.FlitHelper.{connIcn, hwaConn}
import zhujiang.chi.{DataFlit, RReqFlit, RespFlit, RingFlit, SnoopFlit}

class DCacheCoreWrapper (node:Node)(implicit p:Parameters) extends BaseCoreWrapper with HasXSParameter {
  private val coreP = p(XSCoreParamsKey)
  private val icacheP = coreP.icacheParameters
  private val dcache = LazyModule(new DCache)

  private val mmioSourceBits = log2Ceil(icacheP.nMMIOs.max(coreP.UncacheBufferSize)) + 1
  private val cioNode = TLClientNode(Seq(TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "uncache",
      sourceId = IdRange(0, 1 << mmioSourceBits)
    ))
  )))

  private val preftchNode = coreP.prefetcher.map(_ => BundleBridgeSource(() => new PrefetchRecv))

  private val cacheXBar = LazyModule(new TLXbar)

  cacheXBar.node :=* TLBuffer.chainNode(1, Some(s"l1d_buffer")) :=* dcache.clientNode

  //L2 Connections
  private val l2cache = LazyModule(new TL2CHICoupledL2)
  private val tpMetaSinkNode = l2cache.tpmeta_source_node.map(_.makeSink())
  private val tpMetaSourceNode = l2cache.tpmeta_sink_node.map(n => BundleBridgeSource(n.genOpt.get))
  l2cache.tpmeta_sink_node.foreach(_ := tpMetaSourceNode.get)

  l2cache.pf_recv_node.foreach(_ := preftchNode.get)
  l2cache.mmioNode :*= TLBuffer() :*= cioNode
  l2cache.managerNode :=*
    TLBuffer.chainNode(1, Some(s"l2_bank_buffer")) :=*
    TLXbar() :=*
    BankBinder(p(XSCoreParamsKey).L2NBanks, p(L2ParamKey).blockBytes) :*=
    l2cache.node :*=
    TLBuffer() :*=
    cacheXBar.node

  lazy val module = new DcacheCoreWrapperImpl
  val btioParams = BlockTestIOParams(
    cioTlParams = Some(cioNode.edges.out.head.bundle),
    icacheTlParams = None,
    ptwTlParams = None,
    dcacheTlParams = None,
    cmoTlParams = None,
    node = node,
    dcache = true
  )
  @instantiable
  class DcacheCoreWrapperImpl extends BaseCoreWrapperImpl(this, node) with HasZJParams {
    @public
    val btio = IO(new BlockTestIO(btioParams))
    btio.cio.get <> cioNode.out.head._1
    btio.dcsh.get <> dcache.module.io
    btio.mhartid := io.mhartid
    btio.clock := io.clock
    btio.reset := io.reset
    btio.mtip := io.mtip
    btio.msip := io.msip

    l2cache.module.io.hartId := io.mhartid
    l2cache.module.io_nodeID := 0.U(7.W)
    l2cache.module.io.pfCtrlFromCore := DontCare
    l2cache.module.io.l2_tlb_req := DontCare
    l2cache.module.io.debugTopDown := DontCare
    l2cache.module.io.dft.func.foreach(_ := io.dft.toSramBroadCastBundle)
    l2cache.module.io.dft.reset.foreach(_ := io.dft.toResetDftBundle)
    l2cache.module.io.ramctl := io.ramctl

    tpMetaSinkNode.foreach(_.in.head._1.ready := false.B)
    tpMetaSourceNode.foreach(_.out.head._1 := DontCare)

    cpuHalt := false.B

    l2cache.module.io_chi match {
      case l2_chi: coupledL2.tl2chi.DecoupledPortIO =>
        connectByName(txreq, l2_chi.tx.req)
        txreq.bits.SnoopMe := l2_chi.tx.req.bits.snoopMe
        txreq.bits.QoS := 0.U
        connectByName(txrsp, l2_chi.tx.rsp)
        connectByName(txdat, l2_chi.tx.dat)
        connectByName(l2_chi.rx.snp, rxsnp)
        connectByName(l2_chi.rx.rsp, rxrsp)
        connectByName(l2_chi.rx.dat, rxdat)
      case _ => require(requirement = false, "Credit-Grant CHI is not supported!")
    }

    private val assertionNode = HardwareAssertion.placePipe(Int.MaxValue, moduleTop = true).map(_.head)
    HardwareAssertion.release(assertionNode, "hwa", "core")
    assertionNode.foreach(_.hassert.bus.get.ready := true.B)
    if(p(HardwareAssertionKey).enable) {
      val dbgBd = WireInit(0.U.asTypeOf(Decoupled(new RingFlit(debugFlitBits))))
      if(assertionNode.isDefined) {
        dontTouch(assertionNode.get.hassert)
        hwaConn(dbgBd, assertionNode.get.hassert)
      }
      connIcn(pdc.io.icn.rx.debug.get, dbgBd)
    }
  }
}

