package linknan.soc

import chisel3._
import chisel3.util.Cat
import linknan.cluster.hub.interconnect.ClusterIcnBundle
import linknan.cluster.hub.peripheral.AclintAddrRemapper
import linknan.soc.device.DevicesWrapper
import linknan.utils.{BitSynchronizer, ClkDiv2}
import org.chipsalliance.cde.config.Parameters
import xs.utils.ResetGen
import xs.utils.sram.SramCtrlBundle
import zhujiang.axi.{AxiBufferChain, AxiBundle, AxiParams, AxiUtils, AxiWidthAdapter, BaseAxiXbar}
import zhujiang.chi.NodeIdBundle
import zhujiang._

class AxiNto1XBar(mst: Seq[AxiParams])(implicit val p: Parameters) extends BaseAxiXbar(mst) with HasZJParams {
  val slvMatchersSeq = Seq((_: UInt) => true.B)
  initialize()
}
class Axi1toNXBar(mst: AxiParams, slvM:Seq[UInt => Bool])(implicit val p: Parameters) extends BaseAxiXbar(Seq(mst)) with HasZJParams {
  val slvMatchersSeq = slvM
  initialize()
}

class LnCrg extends Module {
  val io = IO(new Bundle {
    val in_clk = Input(Clock())
    val out_clk_full = Output(Clock())
    val out_clk_div2 = Output(Clock())
  })
  private val clk_div_2 = Module(new ClkDiv2)
  clk_div_2.io.clock := io.in_clk
  io.out_clk_full := io.in_clk
  io.out_clk_div2 := clk_div_2.io.out
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
  private val ucPort = if(noc.ddrIO.exists(_.params.attr.contains("uc"))) Some(noc.ddrIO.filter(_.params.attr.contains("uc")).head) else None

  private val sAxiFabric = Module(new SlvAxiFabric(dmaPort.params))
  private val devWrp = Module(new DevicesWrapper(cfgPort.params, sAxiFabric.s_axi.sba.params))
  private val maxiFabric = Module(new MstAxiFabric(devWrp.io.ext.cfg.params, ucPort.map(_.params)))

  devWrp.io.slv <> cfgPort
  maxiFabric.s_axi.cfg <> devWrp.io.ext.cfg
  maxiFabric.s_axi.uc.foreach(_ <> ucPort.get)
  sAxiFabric.s_axi.sba <> devWrp.io.mst
  dmaPort <> sAxiFabric.m_axi.dma

  val ddrDrv = noc.ddrIO.filterNot(axi => axi.params.attr.contains("uc")).map(AxiUtils.getIntnl) ++ maxiFabric.m_axi.uc
  val cfgDrv = noc.cfgIO.filterNot(axi => axi.params.attr.contains("main") || axi.params.attr.contains("pcie")).map(AxiUtils.getIntnl) :+ maxiFabric.m_axi.cfg
  val dmaDrv = noc.dmaIO.filterNot(_.params.attr.contains("main")).map(AxiUtils.getIntnl) ++ Seq(sAxiFabric.s_axi.cfg, sAxiFabric.s_axi.dat)
  val ccnDrv = Seq()
  val hwaDrv = noc.hwaIO.map(AxiUtils.getIntnl)
  runIOAutomation()

  private val clusterNum = noc.ccnIO.size
  val io = IO(new Bundle {
    val reset = Input(AsyncReset())
    val cluster_clocks = Input(Vec(clusterNum, Clock()))
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(p(LinkNanParamsKey).nrExtIntr.W))
    val ci = Input(UInt(ciIdBits.W))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = devWrp.io.debug.systemjtag.map(t => chiselTypeOf(t))
    val dft = new LnDftWires
    val ramctl = Input(new SramCtrlBundle)
  })
  val cluster = noc.ccnIO.map(ccn => IO(new ClusterIcnBundle(ccn.node)))
  private val crg = Module(new LnCrg)
  crg.io.in_clk := io.noc_clock
  cluster.foreach(c => dontTouch(c))
  implicitClock := crg.io.out_clk_full
  implicitReset := withReset(io.reset){ResetGen(dft = Some(io.dft.toResetDftBundle))}

  private val rtcSync = BitSynchronizer(io.rtc_clock)
  private val rtcEn = RegNext(!noc.io.onReset)
  private val rtcClockGated = rtcSync & rtcEn
  devWrp.io.ext.intr := io.ext_intr
  devWrp.io.ci := io.ci
  devWrp.io.debug.systemjtag.foreach({ jtag =>
    jtag <> io.jtag.get
    jtag.reset := withClockAndReset(io.jtag.get.jtag.TCK, io.jtag.get.reset) {ResetGen(3, Some(io.dft.toResetDftBundle))}
  })
  devWrp.io.dft.from(io.dft)
  devWrp.io.debug.dmactiveAck := devWrp.io.debug.dmactive
  devWrp.io.debug.clock := DontCare
  devWrp.io.debug.reset := DontCare
  devWrp.full_clock := crg.io.out_clk_full
  devWrp.div2_clock := crg.io.out_clk_div2
  devWrp.reset := implicitReset
  io.ndreset := devWrp.io.debug.ndreset
  noc.io.ci := io.ci
  noc.io.dft.from(io.dft)
  noc.io.dft.llc <> io.dft.noc
  noc.io.ramctl := io.ramctl

  for((ext, noc) <- cluster.zip(noc.ccnIO)) {
    val node = ext.socket.node
    val clusterId = node.clusterId
    ext.cpu_clock := io.cluster_clocks(clusterId)
    ext.noc_clock := crg.io.out_clk_full
    ext.dft.from(io.dft)
    ext.dft.core <> io.dft.core(clusterId)
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
    }
  }
}
