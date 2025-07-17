package linknan.cluster

import chisel3._
import chisel3.experimental.hierarchy.core.IsLookupable
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp, LazyRawModuleImp}
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters, TLClientNode, TLEdgeIn, TLManagerNode, TLXbar}
import linknan.cluster.core.{DCacheCoreWrapper, NanhuCoreWrapper, NoCoreWrapper}
import linknan.cluster.hub.{AlwaysOnDomain, ImsicBundle}
import linknan.cluster.hub.interconnect.ClusterDeviceBundle
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.cache.DCacheIO
import xijiang.{Node, NodeType}
import zhujiang.{ZJParametersKey, ZJRawModule}
import difftest.gateway._

class BlockTestIO(val params:BlockTestIOParams)(implicit p:Parameters) extends Bundle {
  val clock = Output(Clock())
  val reset = Output(AsyncReset())
  val cio = params.cioTlParams.map(tp => Flipped(new TLBundle(tp)))
  val cmo = params.cmoTlParams.map(tp => Flipped(new TLBundle(tp)))
  val icache = params.icacheTlParams.map(tp => Flipped(new TLBundle(tp)))
  val ptw = params.ptwTlParams.map(tp => Flipped(new TLBundle(tp)))
  val dcache = params.dcacheTlParams.map(tp => Flipped(new TLBundle(tp)))
  val dcsh = Option.when(params.dcache)(new DCacheIO)
  val mhartid = Output(UInt(p(ZJParametersKey).clusterIdBits.W))
  val mtip = Output(Bool())
  val msip = Output(Bool())
}

case class BlockTestIOParams(
  cioTlParams:Option[TLBundleParameters],
  icacheTlParams: Option[TLBundleParameters],
  ptwTlParams: Option[TLBundleParameters],
  dcacheTlParams: Option[TLBundleParameters],
  cmoTlParams: Option[TLBundleParameters],
  node:Node,
  dcache: Boolean
) extends IsLookupable

class CpuCluster(node:Node)(implicit p:Parameters) extends LazyModule {
  private val removeCore = p(LinkNanParamsKey).removeCore
  private val keepL1c = p(LinkNanParamsKey).keepL1c
  private val tile = if(removeCore && keepL1c) {
    LazyModule(new DCacheCoreWrapper(node.copy(nodeType = NodeType.RF)))
  } else if(removeCore && !keepL1c) {
    LazyModule(new NoCoreWrapper(node.copy(nodeType = NodeType.RF)))
  } else {
    LazyModule(new NanhuCoreWrapper(node.copy(nodeType = NodeType.RF)))
  }

  val btIoParams = tile match {
    case w: NoCoreWrapper => Some(w.btioParams)
    case w: DCacheCoreWrapper => Some(w.btioParams)
    case _ => None
  }
  lazy val module = new CpuClusterImpl
  @instantiable
  class CpuClusterImpl(implicit p:Parameters) extends LazyRawModuleImp(this) {
    @public val icn = IO(new ClusterDeviceBundle(node))
    @public val btio = btIoParams.map(bp => IO(new BlockTestIO(bp)))
    private val hub = Module(new AlwaysOnDomain(node))
    icn <> hub.io.icn
    hub.io.cpu <> tile.module.io
    tile match {
      case w: NoCoreWrapper => btio.get <> w.module.btio
      case w: DCacheCoreWrapper => btio.get <> w.module.btio
      case _ => None
    }

    // Difftest
    @public val probeDiff = IO(Output(new CoreGatewayBundle))
    @public val probe_reset = IO(Output(Bool()))
    probe_reset := hub.io.cpu.reset.asBool
    tile match {
      case w: NanhuCoreWrapper => {
        import chisel3.util.experimental._
        import difftest.gateway._
        
        CoreGateway.PrintGateway()

        probeDiff.archEvent := BoringUtils.tapAndRead(CoreGateway.getOne("difftestArchEvent"))
        probeDiff.instrCommit.zipWithIndex.foreach {
          case (cmt, idx) => cmt := BoringUtils.tapAndRead(CoreGateway.getOne(s"difftestInstrCommit_${idx}"))
        }
        probeDiff.intWriteback.zipWithIndex.foreach {
          case (wb, idx) => wb := BoringUtils.tapAndRead(CoreGateway.getOne(s"difftestIntWriteback_${idx}"))
        }
        probeDiff.fpWriteback.zipWithIndex.foreach {
          case (wb, idx) => wb := BoringUtils.tapAndRead(CoreGateway.getOne(s"difftestFpWriteback_${idx}"))
        }
        probeDiff.vecWriteback.zipWithIndex.foreach {
          case (wb, idx) => wb := BoringUtils.tapAndRead(CoreGateway.getOne(s"difftestVecWriteback_${idx}"))
        }
        probeDiff.vecV0Writeback.zipWithIndex.foreach {
          case (wb, idx) => wb := BoringUtils.tapAndRead(CoreGateway.getOne(s"difftestVecV0Writeback_${idx}"))
        }
        probeDiff.csrState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestCSRState"))
        probeDiff.hcsrState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestHCSRState"))
        probeDiff.debugMode := BoringUtils.tapAndRead(CoreGateway.getOne("difftestDebugMode"))
        probeDiff.triggerCSRState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestTriggerCSRState"))
        probeDiff.fpCSRState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestFpCSRState"))
        probeDiff.archIntRegState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestArchIntRegState"))
        probeDiff.archFpRegState := BoringUtils.tapAndRead(CoreGateway.getOne("difftestArchFpRegState"))
        probeDiff.nonRegInterruptPendingEvent := BoringUtils.tapAndRead(CoreGateway.getOne("difftestNonRegInterruptPendingEvent"))
        probeDiff.trapEvent := BoringUtils.tapAndRead(CoreGateway.getOne("difftestTrapEvent"))
        probeDiff.lrscEvent := BoringUtils.tapAndRead(CoreGateway.getOne("difftestLrScEvent"))
      }
      case _ => probeDiff := DontCare
    }
  }
}
