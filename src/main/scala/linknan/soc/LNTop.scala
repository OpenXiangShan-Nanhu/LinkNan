package linknan.soc

import chisel3._
import chisel3.experimental.hierarchy.{Definition, Instance}
import chisel3.experimental.{ChiselAnnotation, annotate}
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import linknan.cluster.{BlockTestIO, CpuCluster}
import linknan.generator.{MiscKey, TestIoOptionsKey}
import zhujiang.{DftWires, NocIOHelper, ZJModule}
import org.chipsalliance.cde.config.Parameters
import sifive.enterprise.firrtl.NestedPrefixModulesAnnotation
import zhujiang.axi.AxiUtils

class LNTop(implicit p:Parameters) extends ZJModule with NocIOHelper {
  private val mod = this.toNamed
  annotate(new ChiselAnnotation {
    def toFirrtl = NestedPrefixModulesAnnotation(mod, p(MiscKey).prefix, inclusive = true)
  })
  private val uncore = Module(new UncoreTop)
  private val clusterNum = uncore.cluster.size

  val io = IO(new Bundle{
    val cluster_clocks = Input(Vec(clusterNum, Clock()))
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(p(LinkNanParamsKey).nrExtIntr.W))
    val chip = Input(UInt(nodeAidBits.W))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = uncore.io.jtag.map(t => chiselTypeOf(t))
    val dft = Input(new DftWires)
  })

  val ddrDrv = AxiUtils.getIntnl(uncore.ddrIO)
  val cfgDrv = uncore.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = uncore.dmaIO.map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  runIOAutomation()

  uncore.io.reset := reset
  uncore.io.noc_clock := io.noc_clock
  uncore.io.rtc_clock := io.rtc_clock
  uncore.io.ext_intr := io.ext_intr
  uncore.io.chip := io.chip
  uncore.io.default_reset_vector := io.default_reset_vector
  uncore.io.jtag.foreach(_ <> io.jtag.get)
  uncore.io.dft := io.dft
  io.ndreset := uncore.io.ndreset
  uncore.io.cluster_clocks := io.cluster_clocks

  private val ccnNodeMap = uncore.cluster.map(_.socket).groupBy(_.node.attr)
  private val clusterDefMap = for((name, nodes) <- ccnNodeMap) yield {
    val cdef = Definition(new CpuCluster(nodes.head.node)(p.alterPartial({
      case MonitorsEnabled => false
    })))
    (name, cdef)
  }
  private val cpuNum = uncore.cluster.map(_.socket.node.cpuNum).sum

  val core = if(p(TestIoOptionsKey).doBlockTest) Some(IO(Vec(cpuNum, new BlockTestIO(clusterDefMap.head._2.coreIoParams)))) else None
  val ccns = uncore.cluster.map(_.socket.node)

  for(ccn <- uncore.cluster) {
    val clusterId = ccn.socket.node.clusterId
    val cc = Instance(clusterDefMap(ccn.socket.node.attr))
    cc.icn <> ccn
    ccn.socket.c2cClock.foreach(_ := io.noc_clock)
    cc.icn.socket.c2cClock.foreach(_ := io.noc_clock)
    cc.suggestName(s"cc_${ccn.socket.node.domainId}")
    for(i <- 0 until ccn.socket.node.cpuNum) {
      val cid = clusterId + i
      if(p(TestIoOptionsKey).doBlockTest) core.get(cid) <> cc.core.get(i)
    }
  }
}
