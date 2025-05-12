package linknan.soc

import chisel3._
import chisel3.util.Cat
import linknan.cluster.hub.interconnect.ClusterIcnBundle
import linknan.cluster.hub.peripheral.AclintAddrRemapper
import linknan.soc.device.DevicesWrapper
import org.chipsalliance.cde.config.Parameters
import xs.utils.ResetGen
import xs.utils.sram.SramCtrlBundle
import zhujiang.axi.{AxiBufferChain, AxiBundle, AxiParams, AxiUtils, AxiWidthAdapter, BaseAxiXbar}
import zhujiang.chi.NodeIdBundle
import zhujiang._

class AxiDmaXBar(dmaAxiParams: Seq[AxiParams])(implicit val p: Parameters) extends BaseAxiXbar(dmaAxiParams) with HasZJParams {
  val slvMatchersSeq = Seq((_: UInt) => true.B)
  initialize()
}

class UncoreTop(implicit p:Parameters) extends ZJRawModule with NocIOHelper
  with ImplicitClock with ImplicitReset {
  override protected val implicitClock = Wire(Clock())
  override protected val implicitReset = Wire(AsyncReset())

  private val noc = Module(new Zhujiang)

  require(noc.cfgIO.count(_.params.attr.contains("main")) == 1)
  require(noc.dmaIO.count(_.params.attr.contains("main")) == 1)
  private val cfgPort = noc.cfgIO.filter(_.params.attr.contains("main")).head
  private val dmaPort = noc.dmaIO.filter(_.params.attr.contains("main")).head

  private val dmaXBarP = dmaPort.params
  private val dbgWrpSlvP = dmaXBarP.copy(attr = "debug", idBits = dmaXBarP.idBits - 2)
  private val extCfgSlvP = dmaXBarP.copy(attr = "cfg_s", idBits = dmaXBarP.idBits - 2)
  private val extDmaSlvP = dmaXBarP.copy(attr = "mem_s", idBits = dmaXBarP.idBits - 2)
  private val dmaXBarParams = Seq(dbgWrpSlvP, extCfgSlvP, extDmaSlvP)

  private val devWrp = Module(new DevicesWrapper(cfgPort.params, dbgWrpSlvP))
  private val dmaXBar = Module(new AxiDmaXBar(dmaXBarParams))
  private val cfgWidthAdapter = Module(new AxiWidthAdapter(extCfgSlvP, extCfgSlvP.copy(dataBits = 64), 16))

  private val extSlvMemBuf = Module(new AxiBufferChain(extDmaSlvP, 2))
  private val extSlvCfgBuf = Module(new AxiBufferChain(extCfgSlvP.copy(dataBits = 64), 2))
  private val extMstCfgBuf = Module(new AxiBufferChain(devWrp.io.ext.cfg.params, 2))

  devWrp.io.slv <> cfgPort
  extMstCfgBuf.io.in <> devWrp.io.ext.cfg
  cfgWidthAdapter.io.mst <> extSlvCfgBuf.io.out

  dmaXBar.io.upstream(0) <> devWrp.io.mst
  dmaXBar.io.upstream(1) <> cfgWidthAdapter.io.slv
  dmaXBar.io.upstream(2) <> extSlvMemBuf.io.out
  dmaPort <> dmaXBar.io.downstream.head

  dmaXBar.io.upstream(1).aw.bits.addr := AclintAddrRemapper(cfgWidthAdapter.io.slv.aw.bits.addr)
  dmaXBar.io.upstream(1).aw.bits.cache := "b0000".U
  dmaXBar.io.upstream(1).ar.bits.addr := AclintAddrRemapper(cfgWidthAdapter.io.slv.ar.bits.addr)
  dmaXBar.io.upstream(1).ar.bits.cache := "b0000".U
  dontTouch(dmaXBar.io.upstream(1))

  dmaXBar.io.upstream(2).aw.bits.cache := "b0010".U | extSlvMemBuf.io.out.aw.bits.cache
  dmaXBar.io.upstream(2).ar.bits.cache := "b0010".U | extSlvMemBuf.io.out.ar.bits.cache

  val ddrDrv = noc.ddrIO.map(AxiUtils.getIntnl)
  val cfgDrv = Seq(extMstCfgBuf.io.out) ++ noc.cfgIO.filterNot(_.params.attr.contains("main")).map(AxiUtils.getIntnl)
  val dmaDrv = Seq(extSlvCfgBuf.io.in, extSlvMemBuf.io.in) ++ noc.dmaIO.filterNot(_.params.attr.contains("main")).map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = noc.hwaIO.map(AxiUtils.getIntnl)
  runIOAutomation()

  private val clusterNum = noc.ccnIO.size
  val io = IO(new Bundle {
    val reset = Input(AsyncReset())
    val cluster_clocks = Input(Vec(clusterNum, Clock()))
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(zjParams.externalInterruptNum.W))
    val ci = Input(UInt(ciIdBits.W))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = devWrp.io.debug.systemjtag.map(t => chiselTypeOf(t))
    val dft = Input(new DftWires)
    val ramctl = Input(new SramCtrlBundle)
  })
  val cluster = noc.ccnIO.map(ccn => IO(new ClusterIcnBundle(ccn.node)))
  cluster.foreach(c => dontTouch(c))
  implicitClock := io.noc_clock
  implicitReset := withReset(io.reset){ResetGen(dft = Some(io.dft.reset))}

  private val rtcEn = RegNext(!noc.io.onReset)
  private val rtcClockGated = io.rtc_clock & rtcEn
  devWrp.io.ext.intr := io.ext_intr
  devWrp.io.ci := io.ci
  devWrp.io.debug.systemjtag.foreach(_ <> io.jtag.get)
  devWrp.io.dft := io.dft
  devWrp.io.debug.dmactiveAck := devWrp.io.debug.dmactive
  devWrp.io.debug.clock := DontCare
  devWrp.io.debug.reset := DontCare
  devWrp.clock := io.noc_clock
  devWrp.reset := io.reset
  io.ndreset := devWrp.io.debug.ndreset
  noc.io.ci := io.ci
  noc.io.dft := io.dft
  noc.io.ramctl := io.ramctl

  private var rstIdx = 0
  for(((ext, noc), idx) <- cluster.zip(noc.ccnIO).zipWithIndex) {
    val node = ext.socket.node
    val clusterId = node.clusterId
    if(node.socket == "async") {
      ext.osc_clock := io.cluster_clocks(idx)
    } else {
      ext.osc_clock := io.noc_clock
    }
    ext.dft := io.dft
    ext.ramctl := io.ramctl
    ext.socket <> noc
    ext.misc.clusterId := Cat(io.ci, clusterId.U((clusterIdBits - ciIdBits).W))
    ext.misc.defaultBootAddr := io.default_reset_vector
    ext.misc.nodeNid := noc.node.nodeId.U.asTypeOf(new NodeIdBundle).nid
    ext.misc.rtc := RegNext(rtcClockGated)
    for(i <- 0 until node.cpuNum) {
      val cid = clusterId + i
      ext.misc.meip(i) := devWrp.io.cpu.meip(cid)
      ext.misc.seip(i) := devWrp.io.cpu.seip(cid)
      ext.misc.dbip(i) := devWrp.io.cpu.dbip(cid)
      devWrp.io.resetCtrl.hartIsInReset(cid) := ext.misc.resetState(i)
      rstIdx = rstIdx + 1
    }
  }
}
