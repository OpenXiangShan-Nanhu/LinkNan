package linknan.soc

import chisel3._
import chisel3.experimental.hierarchy.{Definition, Instance}
import chisel3.experimental.{ChiselAnnotation, annotate}
import chisel3.util.{Decoupled, log2Ceil, log2Up}
import coupledL2.tl2chi.{CHIIssue, Issue}
import coupledL2.{BankBitsKey, L1Param, L2Param, L2ParamKey}
import freechips.rocketchip.tile.MaxHartIdBits
import linknan.cluster.{BlockTestIO, CpuCluster}
import org.chipsalliance.cde.config.{Config, Parameters}
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import sifive.enterprise.firrtl.NestedPrefixModulesAnnotation
import xiangshan.{XLen, XSCoreParameters, XSCoreParamsKey}
import xs.utils.debug.HardwareAssertionKey
import xs.utils.perf.{DebugOptionsKey, LogUtilsOptionsKey, PerfCounterOptionsKey}
import zhujiang.axi.AxiUtils
import zhujiang.device.misc.ZJDebugBundle
import zhujiang.{DftWires, NocIOHelper, ZJParametersKey, ZJRawModule}

object GlobalStaticParameters {
  var lnParams:LinkNanParams = null
  var xsParams:XSCoreParameters = null
  var l2Params:L2Param = null
}

class LNTop(implicit p:Parameters) extends ZJRawModule with NocIOHelper {
  GlobalStaticParameters.lnParams = p(LinkNanParamsKey).copy()
  GlobalStaticParameters.xsParams = p(XSCoreParamsKey).copy()
  private val coreP = p(XSCoreParamsKey)
  private val dcacheP = p(XSCoreParamsKey).dcacheParametersOpt.get
  GlobalStaticParameters.l2Params = p(L2ParamKey).copy(
    clientCaches = Seq(L1Param(
      name = "dcache",
      sets = dcacheP.nSets / p(XSCoreParamsKey).L2NBanks,
      ways = dcacheP.nWays,
      aliasBitsOpt = dcacheP.aliasBitsOpt,
      vaddrBitsOpt = Some(coreP.GPAddrBitsSv48x4 - log2Up(dcacheP.blockBytes)),
      isKeywordBitsOpt = dcacheP.isKeywordBitsOpt
    )),
    FPGAPlatform = p(DebugOptionsKey).FPGAPlatform
  )
  private val mod = this.toNamed
  annotate(new ChiselAnnotation {
    def toFirrtl = NestedPrefixModulesAnnotation(mod, p(LinkNanParamsKey).prefix, inclusive = true)
  })
  private val uncore = Module(new UncoreTop()(
    new Config((_,_,_) => {
      case HardwareAssertionKey => p(HardwareAssertionKey)
      case ZJParametersKey => p(ZJParametersKey)
      case LinkNanParamsKey => GlobalStaticParameters.lnParams
      case DebugOptionsKey => p(DebugOptionsKey)
      case MonitorsEnabled => false
      case LogUtilsOptionsKey => p(LogUtilsOptionsKey)
      case PerfCounterOptionsKey => p(PerfCounterOptionsKey)
    })
  ))
  private val clusterNum = uncore.cluster.size

  val io = IO(new Bundle {
    val reset = Input(AsyncReset())
    val cluster_clocks = Input(Vec(clusterNum, Clock()))
    val noc_clock = Input(Clock())
    val rtc_clock = Input(Bool())
    val ext_intr = Input(UInt(p(LinkNanParamsKey).nrExtIntr.W))
    val ci = Input(UInt(ciIdBits.W))
    val ndreset = Output(Bool())
    val default_reset_vector = Input(UInt(raw.W))
    val jtag = uncore.io.jtag.map(t => chiselTypeOf(t))
    val dft = Input(new DftWires)
  })

  val ddrDrv = uncore.ddrIO.map(AxiUtils.getIntnl)
  val cfgDrv = uncore.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = uncore.dmaIO.map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = uncore.hwaIO.map(AxiUtils.getIntnl)
  runIOAutomation()

  uncore.io.reset := io.reset
  uncore.io.noc_clock := io.noc_clock
  uncore.io.rtc_clock := io.rtc_clock
  uncore.io.ext_intr := io.ext_intr
  uncore.io.ci := io.ci
  uncore.io.default_reset_vector := io.default_reset_vector
  uncore.io.jtag.foreach(_ <> io.jtag.get)
  uncore.io.dft := io.dft
  io.ndreset := uncore.io.ndreset
  uncore.io.cluster_clocks := io.cluster_clocks

  private val clusterP = new Config((_,_,_) => {
    case HardwareAssertionKey => p(HardwareAssertionKey)
    case XSCoreParamsKey => GlobalStaticParameters.xsParams
    case L2ParamKey => GlobalStaticParameters.l2Params
    case LinkNanParamsKey => GlobalStaticParameters.lnParams
    case LogUtilsOptionsKey => p(LogUtilsOptionsKey)
    case PerfCounterOptionsKey => p(PerfCounterOptionsKey)
    case DebugOptionsKey => p(DebugOptionsKey)
    case ZJParametersKey => p(ZJParametersKey)
    case MonitorsEnabled => false
    case MaxHartIdBits => clusterIdBits
    case XLen => 64
    case BankBitsKey => log2Ceil(GlobalStaticParameters.xsParams.L2NBanks)
    case CHIIssue => Issue.Eb
    case xiangshan.EnableCHI => true
    case coupledL2.EnableCHI => true
  })
  private val ccnNodes = uncore.cluster.map(_.socket)
  private val ccGen = LazyModule(new CpuCluster(ccnNodes.head.node)(clusterP))
  private val ccDef = Definition(ccGen.module)

  val core = Option.when(p(LinkNanParamsKey).removeCore)(IO(Vec(uncore.cluster.size, new BlockTestIO(ccGen.btIoParams.get))))
  val ccns = uncore.cluster.map(_.socket.node)

  for(ccn <- uncore.cluster) {
    val clusterId = ccn.socket.node.clusterId
    val cc = Instance(ccDef)
    cc.icn <> ccn
    ccn.socket.c2cClock.foreach(_ := io.noc_clock)
    cc.icn.socket.c2cClock.foreach(_ := io.noc_clock)
    cc.suggestName(s"cc_${ccn.socket.node.domainId}")
    for(i <- 0 until ccn.socket.node.cpuNum) {
      val cid = clusterId + i
      if(p(LinkNanParamsKey).removeCore) core.get(cid) <> cc.btio.get
    }
  }
  linknan.devicetree.DeviceTreeGenerator.generate(clusterP)
}
