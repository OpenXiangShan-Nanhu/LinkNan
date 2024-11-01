package linknan.cluster

import SimpleL2.Configs.L2ParamKey
import SimpleL2.chi.CHIBundleParameters
import chisel3._
import chisel3.experimental.hierarchy.core.IsLookupable
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}
import darecreek.exu.vfu.{VFuParameters, VFuParamsKey}
import freechips.rocketchip.diplomacy.{LazyModule, MonitorsEnabled}
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters}
import linknan.generator.RemoveCoreKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import xijiang.Node
import xs.utils.tl.{TLUserKey, TLUserParams}
import xs.utils.{ClockManagerWrapper, ResetGen}
import zhujiang.{ZJParametersKey, ZJRawModule}
import linknan.cluster.interconnect.ClusterDeviceBundle

class CoreBlockTestIO(params:CoreBlockTestIOParams)(implicit p:Parameters) extends Bundle {
  val clock = Output(Clock())
  val reset = Output(AsyncReset())
  val cio = Flipped(new TLBundle(params.ioParams))
  val l2 = Flipped(new TLBundle(params.l2Params))
  val mhartid = Output(UInt(p(ZJParametersKey).clusterIdBits.W))
}

case class CoreBlockTestIOParams(ioParams:TLBundleParameters, l2Params: TLBundleParameters) extends IsLookupable

@instantiable
class CpuCluster(node:Node)(implicit p:Parameters) extends ZJRawModule {
  private val removeCore = p(RemoveCoreKey)
  private val dcacheParams = p(XSCoreParamsKey).dcacheParametersOpt.get
  private val l2Params = p(L2ParamKey)

  private val coreGen = LazyModule(new CoreWrapper()(p.alterPartial({
    case MonitorsEnabled => false
    case TLUserKey => TLUserParams(aliasBits = dcacheParams.aliasBitsOpt.getOrElse(0))
    case VFuParamsKey => VFuParameters()
  })))
  private val coreDef = if(!removeCore) Some(Definition(coreGen.module)) else None
  private val coreSeq = if(!removeCore) Some(Seq.fill(node.cpuNum)(Instance(coreDef.get))) else None
  coreSeq.foreach(_.zipWithIndex.foreach({case(c, i) => c.suggestName(s"core_$i")}))
  private val cioEdge = coreGen.cioNode.edges.in.head
  private val cl2Edge = coreGen.l2Node.edges.in.head

  private val csu = LazyModule(new ClusterSharedUnit(cioEdge, cl2Edge, node)(p.alterPartial({
    case MonitorsEnabled => false
    case TLUserKey => TLUserParams(aliasBits = dcacheParams.aliasBitsOpt.getOrElse(0))
    case L2ParamKey => l2Params.copy(
      nrClients = node.cpuNum,
      chiBundleParams = Some(CHIBundleParameters(
        nodeIdBits = niw,
        addressBits = raw
      ))
    )
  })))
  private val _csu = Module(csu.module)

  @public val coreIoParams = CoreBlockTestIOParams(cioEdge.bundle, cl2Edge.bundle)
  @public val icn = IO(new ClusterDeviceBundle(node))
  @public val core = if(removeCore) Some(IO(Vec(node.cpuNum, new CoreBlockTestIO(coreIoParams)))) else None

  private val pll = if(p(ZJParametersKey).cpuAsync) Some(Module(new ClockManagerWrapper)) else None
  private val resetSync = withClockAndReset(_csu.io.clock, icn.ccn.reset) { ResetGen(dft = Some(icn.dft.reset)) }

  icn <> _csu.io.icn

  if(pll.isDefined) {
    _csu.io.pllLock := pll.get.io.lock
    pll.get.io.cfg := _csu.io.pllCfg
    pll.get.io.in_clock := icn.osc_clock
    _csu.io.clock := pll.get.io.cpu_clock
  } else {
    _csu.io.pllLock := true.B
    _csu.io.clock := icn.osc_clock
  }
  _csu.io.reset := resetSync

  if(removeCore) {
    for(i <- 0 until node.cpuNum) {
      core.get(i).l2 <> _csu.io.core(i).l2
      core.get(i).cio <> _csu.io.core(i).cio
      core.get(i).reset := _csu.io.core(i).reset
      core.get(i).clock <> _csu.io.core(i).clock
      core.get(i).mhartid <> _csu.io.core(i).mhartid
      _csu.io.core(i).halt := false.B
      _csu.io.core(i).icacheErr := DontCare
      _csu.io.core(i).dcacheErr := DontCare
      _csu.io.core(i).reset_state := false.B
    }
  } else {
    for(i <- 0 until node.cpuNum) _csu.io.core(i) <> coreSeq.get(i).io
  }
}