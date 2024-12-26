package linknan.soc

import chisel3._
import chisel3.experimental.hierarchy.{Definition, Instance}
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util._
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import linknan.cluster.{BlockTestIO, CpuCluster}
import linknan.generator.{MiscKey, TestIoOptionsKey}
import linknan.soc.uncore.UncoreComplex
import zhujiang.{DftWires, ZJParametersKey, ZJRawModule, Zhujiang}
import org.chipsalliance.cde.config.Parameters
import sifive.enterprise.firrtl.NestedPrefixModulesAnnotation
import zhujiang.axi.AxiBundle

class LNTop(implicit p:Parameters) extends ZJRawModule with ImplicitClock with ImplicitReset {
  override protected val implicitClock = Wire(Clock())
  implicitClock := false.B.asClock
  override protected val implicitReset = Wire(AsyncReset())
  private val mod = this.toNamed
  annotate(new ChiselAnnotation {
    def toFirrtl = NestedPrefixModulesAnnotation(mod, p(MiscKey).prefix, inclusive = true)
  })
  private val noc = Module(new Zhujiang)
  private val clusterNum = noc.io.ccn.length
  private val uncore = Module(new UncoreComplex(noc.io.soc.cfg.node, noc.io.soc.dma.node))
  uncore.io.icn <> noc.io.soc

  val io = IO(new Bundle{
    val reset = Input(AsyncReset())
    val cluster_clocks = if(p(ZJParametersKey).cpuAsync) Some(Input(Vec(clusterNum, Clock()))) else None
    val soc_clock = Input(Clock())
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(zjParams.externalInterruptNum.W))
    val chip = Input(UInt(nodeAidBits.W))
    val ddr = new AxiBundle(noc.io.ddr.params)
    val cfg = new AxiBundle(uncore.io.ext.cfg.params)
    val dma = Flipped(new AxiBundle(uncore.io.ext.dma.params))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = chiselTypeOf(uncore.io.debug.systemjtag.get)
  })
  val dft = IO(Input(new DftWires))
  implicitReset := io.reset

  io.ddr <> noc.io.ddr
  noc.io.chip := io.chip
  noc.dft := dft
  noc.clock := io.noc_clock

  uncore.io.ext.dma <> io.dma
  io.cfg <> uncore.io.ext.cfg
  uncore.io.ext.timerTick := io.rtc_clock
  uncore.io.ext.intr := io.ext_intr
  uncore.io.chip := io.chip
  uncore.io.debug.systemjtag.foreach(_ <> io.jtag)
  uncore.clock := io.soc_clock
  uncore.dft := dft
  uncore.io.debug.dmactiveAck := uncore.io.debug.dmactive
  uncore.io.debug.clock := DontCare
  uncore.io.debug.reset := DontCare
  io.ndreset := uncore.io.debug.ndreset

  private val ccnNodeMap = noc.io.ccn.groupBy(_.node.attr)
  private val clusterDefMap = for((name, nodes) <- ccnNodeMap) yield {
    val cdef = Definition(new CpuCluster(nodes.head.node)(p.alterPartial({
      case MonitorsEnabled => false
    })))
    (name, cdef)
  }
  private val cpuNum = noc.io.ccn.map(_.node.cpuNum).sum

  val core = if(p(TestIoOptionsKey).doBlockTest) Some(IO(Vec(cpuNum, new BlockTestIO(clusterDefMap.head._2.coreIoParams)))) else None
  val ccns = noc.io.ccn.map(_.node)

  for((ccn, i) <- noc.io.ccn.zipWithIndex) {
    val clusterId = ccn.node.clusterId
    val cc = Instance(clusterDefMap(ccn.node.attr))
    cc.icn.ccn <> ccn
    if(p(ZJParametersKey).cpuAsync) {
      cc.icn.osc_clock := io.cluster_clocks.get(i)
    } else {
      cc.icn.osc_clock := io.noc_clock
    }
    cc.icn.dft := dft
    for(i <- 0 until ccn.node.cpuNum) {
      val cid = clusterId + i
      cc.icn.misc.msip(i) := uncore.io.cpu.msip(cid)
      cc.icn.misc.mtip(i) := uncore.io.cpu.mtip(cid)
      cc.icn.misc.meip(i) := uncore.io.cpu.meip(cid)
      cc.icn.misc.seip(i) := uncore.io.cpu.seip(cid)
      cc.icn.misc.dbip(i) := uncore.io.cpu.dbip(cid)
      cc.icn.misc.mhartid(i) := Cat(io.chip, cid.U((clusterIdBits - nodeAidBits).W))
      uncore.io.resetCtrl.hartIsInReset(cid) := cc.icn.misc.resetState(i)
      cc.icn.misc.defaultBootAddr(i) := io.default_reset_vector
      if(cid == 0 || p(TestIoOptionsKey).removeCore || p(TestIoOptionsKey).removeCsu) {
        cc.icn.misc.defaultCpuEnable(i) := true.B
      } else {
        cc.icn.misc.defaultCpuEnable(i) := false.B
      }
      if(p(TestIoOptionsKey).doBlockTest) core.get(cid) <> cc.core.get(i)
    }
  }
}
