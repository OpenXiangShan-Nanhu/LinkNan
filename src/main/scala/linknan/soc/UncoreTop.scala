package linknan.soc

import chisel3._
import chisel3.util.Cat
import linknan.cluster.hub.interconnect.ClusterIcnBundle
import linknan.generator.TestIoOptionsKey
import linknan.soc.device.DevicesWrapper
import org.chipsalliance.cde.config.Parameters
import xs.utils.debug.{DomainInfo, HardwareAssertion}
import zhujiang.axi.{AxiParams, BaseAxiXbar}
import zhujiang.{DftWires, HasZJParams, NocIOHelper, ZJRawModule, Zhujiang}

class AxiDmaXBar(dmaAxiParams: Seq[AxiParams])(implicit val p: Parameters) extends BaseAxiXbar(dmaAxiParams) with HasZJParams {
  val slvMatchersSeq = Seq((_: UInt) => true.B)
  initialize()
}

class UncoreTop(implicit p:Parameters) extends ZJRawModule with NocIOHelper
  with ImplicitClock with ImplicitReset {
  override protected val implicitClock = Wire(Clock())
  override protected val implicitReset = Wire(AsyncReset())

  private val noc = Module(new Zhujiang)
  HardwareAssertion.fromDomain(noc.io.assertionOut, noc.assertionInfo, level = 0, s"noc")
  HardwareAssertion.placePipe(1)
  private val assertionNode = HardwareAssertion.placePipe(Int.MaxValue, moduleTop = true)

  require(noc.cfgIO.count(_.params.attr == "main") == 1)
  require(noc.dmaIO.count(_.params.attr == "main") == 1)
  private val cfgPort = noc.cfgIO.filter(_.params.attr == "main").head
  private val dmaPort = noc.dmaIO.filter(_.params.attr == "main").head
  private val devWrp = Module(new DevicesWrapper(cfgPort.params, dmaPort.params, assertionNode))
  private val dmaXBar = Module(new AxiDmaXBar(Seq.fill(2)(devWrp.io.mst.params)))

  dmaXBar.io.upstream.head <> devWrp.io.mst
  devWrp.io.slv <> cfgPort
  dmaPort <> dmaXBar.io.downstream.head

  val ddrDrv = noc.ddrIO
  val cfgDrv = Seq(devWrp.io.ext.cfg) ++ noc.cfgIO.filterNot(_.params.attr == "main")
  val dmaDrv = Seq(dmaXBar.io.upstream.last) ++ noc.dmaIO.filterNot(_.params.attr == "main")
  val ccnDrv = Seq()
  runIOAutomation()

  private val clusterNum = noc.ccnIO.size
  val io = IO(new Bundle {
    val reset = Input(AsyncReset())
    val cluster_clocks = Input(Vec(clusterNum, Clock()))
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(zjParams.externalInterruptNum.W))
    val chip = Input(UInt(nodeAidBits.W))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = devWrp.io.debug.systemjtag.map(t => chiselTypeOf(t))
    val dft = Input(new DftWires)
  })
  val cluster = noc.ccnIO.map(ccn => IO(new ClusterIcnBundle(ccn.node)))
  cluster.foreach(c => dontTouch(c))
  implicitClock := io.noc_clock
  implicitReset := io.reset

  devWrp.io.ext.timerTick := io.rtc_clock
  devWrp.io.ext.intr := io.ext_intr
  devWrp.io.chip := io.chip
  devWrp.io.debug.systemjtag.foreach(_ <> io.jtag.get)
  devWrp.clock := io.noc_clock
  devWrp.io.dft := io.dft
  devWrp.io.debug.dmactiveAck := devWrp.io.debug.dmactive
  devWrp.io.debug.clock := DontCare
  devWrp.io.debug.reset := DontCare
  devWrp.io.hwa := assertionNode.assertion
  devWrp.clock := io.noc_clock
  devWrp.reset := io.reset
  io.ndreset := devWrp.io.debug.ndreset
  noc.io.chip := io.chip
  noc.io.dft := io.dft

  HardwareAssertion.setTopNode(assertionNode)
  HardwareAssertion.release("hwa")

  for(((ext, noc), idx) <- cluster.zip(noc.ccnIO).zipWithIndex) {
    val node = ext.socket.node
    val clusterId = node.clusterId
    if(node.socket == "async") {
      ext.osc_clock := io.cluster_clocks(idx)
    } else {
      ext.osc_clock := io.noc_clock
    }
    ext.dft := io.dft
    ext.socket <> noc
    ext.misc.clusterId := Cat(io.chip, clusterId.U((clusterIdBits - nodeAidBits).W))
    ext.misc.defaultBootAddr := io.default_reset_vector
    for(i <- 0 until node.cpuNum) {
      val cid = clusterId + i
      ext.misc.msip(i) := devWrp.io.cpu.msip(cid)
      ext.misc.mtip(i) := devWrp.io.cpu.mtip(cid)
      ext.misc.meip(i) := devWrp.io.cpu.meip(cid)
      ext.misc.seip(i) := devWrp.io.cpu.seip(cid)
      ext.misc.dbip(i) := devWrp.io.cpu.dbip(cid)
      devWrp.io.resetCtrl.hartIsInReset(cid) := ext.misc.resetState(i)
    }
  }
}
