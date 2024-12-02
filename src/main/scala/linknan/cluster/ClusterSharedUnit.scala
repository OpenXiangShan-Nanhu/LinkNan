package linknan.cluster

import chisel3._
import chisel3.util._
import SimpleL2.Configs.L2ParamKey
import SimpleL2.SimpleL2CacheDecoupled
import SimpleL2.chi.CHIBundleParameters
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tilelink._
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.resources.BindingScope
import linknan.cluster.core.CoreWrapperIO
import linknan.cluster.hub.AlwaysOnDomainBundle
import linknan.cluster.power.controller.{PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.PChannelSlv
import linknan.generator.DcacheKey
import linknan.utils._
import org.chipsalliance.cde.config.Parameters
import xiangshan.{HasXSParameter, XSCoreParamsKey}
import xijiang.Node
import xijiang.router.base.DeviceIcnBundle
import xs.utils.{ClockGate, ResetGen}
import xs.utils.tl.{TLUserKey, TLUserParams}
import zhujiang.HasZJParams
import zhujiang.chi._

class ClusterSharedUnit(cioEdge: TLEdgeIn, l2EdgeIn: TLEdgeIn, node:Node)(implicit p:Parameters) extends LazyModule with HasZJParams
  with BindingScope {
  private val dcacheParams = p(DcacheKey)
  private val l2Params = p(L2ParamKey)
  private val l2cache = LazyModule(new SimpleL2CacheDecoupled()(p.alterPartial({
    case TLUserKey => TLUserParams(aliasBits = dcacheParams.aliasBitsOpt.getOrElse(0))
    case L2ParamKey => l2Params.copy(
      nrClients = node.cpuNum,
      alwaysWriteBackFull = true,
      chiBundleParams = Some(CHIBundleParameters(
        nodeIdBits = niw,
        addressBits = raw
      ))
    )
  })))
  private val l2Xbar = LazyModule(new TLXbar)
  private val l2binder = LazyModule(new BankBinder(64 * (l2Params.nrSlice - 1)))
  private val l2EccIntSink = IntSinkNode(IntSinkPortSimple(1, 1))
  private val l2param = p(L2ParamKey)
  private val cachePortNodes = Seq.fill(node.cpuNum)(TLClientNode(Seq(l2EdgeIn.master)))
  cachePortNodes.zipWithIndex.foreach(n => l2Xbar.node :*= TLBuffer.chainNode(1, Some(s"l2_in_buffer_${n._2}")) :*= n._1)
  l2binder.node :*= l2Xbar.node
  for(i <- 0 until l2param.nrSlice) l2cache.sinkNodes(i) :*= TLBuffer.chainNode(2, Some(s"l2_bank_buffer")) :*= l2binder.node
  l2EccIntSink := l2cache.eccIntNode

  private val cioInNodes = Seq.fill(node.cpuNum)(TLClientNode(Seq(cioEdge.master)))
  private val cioXbar = LazyModule(new TLXbar)
  val cioOutNode = TLManagerNode(Seq(cioEdge.slave))
  cioInNodes.zipWithIndex.foreach(n => cioXbar.node :*= TLBuffer.chainNode(1, Some(s"cio_in_buffer_${n._2}")) :*= n._1)
  cioOutNode :*= TLBuffer.chainNode(1, Some(s"cio_out_buffer")) :*= cioXbar.node

  lazy val module = new Impl
  class Impl extends LazyRawModuleImp(this) with ImplicitClock with ImplicitReset {
    override def provideImplicitClockToLazyChildren = true
    val io = IO(new Bundle{
      val core = Vec(node.cpuNum, Flipped(new CoreWrapperIO(cioEdge.bundle, l2EdgeIn.bundle)))
      val hub = Flipped(new AlwaysOnDomainBundle(node, cioOutNode.in.head._2.bundle))
    })
    private val resetSync = withClockAndReset(io.hub.csu.clock, io.hub.csu.reset.asAsyncReset) { ResetGen(dft = Some(io.hub.csu.dft.reset))}
    childClock := io.hub.csu.clock
    childReset := resetSync
    def implicitClock = childClock
    def implicitReset = childReset

    private val pSlv = Module(new PChannelSlv(devActiveBits, PowerMode.powerModeBits))
    pSlv.io.p <> io.hub.csu.pchn
    pSlv.io.resp.valid := pSlv.io.req.valid
    pSlv.io.resp.bits := true.B
    pSlv.io.active := Cat(true.B, true.B, true.B)
    io.hub.csu.pwrEnAck := io.hub.csu.pwrEnReq
    dontTouch(io.hub.csu.pwrEnAck)
    dontTouch(io.hub.csu.pwrEnReq)
    dontTouch(io.hub.csu.isoEn)
    dontTouch(pSlv.io)

    for(i <- 0 until node.cpuNum) {
      val core = io.core(i)
      val coreCtl = io.hub.cpu(i)
      val coreCg = Module(new ClockGate)
      cachePortNodes(i).out.head._1 <> core.l2
      cioInNodes(i).out.head._1 <> core.cio
      coreCg.io.CK := io.hub.csu.clock
      coreCg.io.E := coreCtl.pcsm.clkEn
      coreCg.io.TE := false.B
      core.clock := coreCg.io.Q
      core.reset := withClockAndReset(io.hub.csu.clock, coreCtl.pcsm.reset.asAsyncReset) { ResetGen(dft = Some(io.hub.csu.dft.reset)) }
      core.isoEn := coreCtl.pcsm.isoEn
      core.pwrEnReq := coreCtl.pcsm.pwrReq
      coreCtl.pcsm.pwrResp := core.pwrEnAck
      core.pchn <> coreCtl.pchn
      coreCtl.icacheErr := core.icacheErr
      coreCtl.dcacheErr := core.dcacheErr
      core.mhartid := coreCtl.mhartid
      core.msip := coreCtl.msip
      core.mtip := coreCtl.mtip
      core.meip := coreCtl.meip
      core.seip := coreCtl.seip
      core.dbip := coreCtl.dbip
      core.imsic <> coreCtl.imsic
      core.reset_vector := coreCtl.reset_vector
      core.dft := io.hub.csu.dft
      val probeValid = cachePortNodes(i).out.head._1.b.valid
      core.l2.b.valid := !coreCtl.blockProbe & probeValid
      cachePortNodes(i).out.head._1.b.ready := core.l2.b.ready & !coreCtl.blockProbe
      coreCtl.reset_state := core.reset_state
      coreCtl.pchn.active := core.pchn.active | RegNext(Cat(probeValid, !probeValid, false.B))
    }
    io.hub.csu.cio <> cioOutNode.in.head._1
    io.hub.csu.cio.a.bits.address := cioOutNode.in.head._1.a.bits.address | (1L << (raw - 1)).U

    private val l2 = l2cache.module
    private val l2Chi = Wire(new DeviceIcnBundle(node))
    private val txreq = Wire(Decoupled(new ReqFlit))
    private val txrsp = Wire(Decoupled(new RespFlit))
    private val txdat = Wire(Decoupled(new DataFlit))
    private val rxrsp = Wire(Decoupled(new RespFlit))
    private val rxdat = Wire(Decoupled(new DataFlit))
    private val rxsnp = Wire(Decoupled(new SnoopFlit))
    
    io.hub.csu.l2cache <> l2Chi

    l2Chi.tx.req.get.valid := txreq.valid
    txreq.ready := l2Chi.tx.req.get.ready
    l2Chi.tx.req.get.bits := txreq.bits.asTypeOf(l2Chi.tx.req.get.bits)

    l2Chi.tx.resp.get.valid := txrsp.valid
    txrsp.ready := l2Chi.tx.resp.get.ready
    l2Chi.tx.resp.get.bits := txrsp.bits.asTypeOf(l2Chi.tx.resp.get.bits)

    l2Chi.tx.data.get.valid := txdat.valid
    txdat.ready := l2Chi.tx.data.get.ready
    l2Chi.tx.data.get.bits := txdat.bits.asTypeOf(l2Chi.tx.data.get.bits)

    rxrsp.valid := l2Chi.rx.resp.get.valid
    l2Chi.rx.resp.get.ready := rxrsp.ready
    rxrsp.bits := l2Chi.rx.resp.get.bits.asTypeOf(rxrsp.bits)

    rxdat.valid := l2Chi.rx.data.get.valid
    l2Chi.rx.data.get.ready := rxdat.ready
    rxdat.bits := l2Chi.rx.data.get.bits.asTypeOf(rxdat.bits)

    rxsnp.valid := l2Chi.rx.snoop.get.valid
    l2Chi.rx.snoop.get.ready := rxsnp.ready
    rxsnp.bits := l2Chi.rx.snoop.get.bits.asTypeOf(rxsnp.bits)

    connectByName(txreq, l2.io.chi.txreq)
    connectByName(txrsp, l2.io.chi.txrsp)
    connectByName(txdat, l2.io.chi.txdat)
    connectByName(l2.io.chi.rxrsp, rxrsp)
    connectByName(l2.io.chi.rxdat, rxdat)
    connectByName(l2.io.chi.rxsnp, rxsnp)
    txreq.bits.LPID := l2.io.chi.txreq.bits.lpID
    txreq.bits.SnoopMe := l2.io.chi.txreq.bits.snoopMe
    txdat.bits.FwdState := l2.io.chi.txdat.bits.fwdState
    l2.io.chi.rxdat.bits.fwdState := rxdat.bits.FwdState

    l2.io.chi_tx_rxsactive := true.B
    l2.io.chi_tx_linkactiveack := true.B
    l2.io.chi_rx_linkactivereq := true.B
    l2.io.nodeID := DontCare
  }
}
