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
  }
}
