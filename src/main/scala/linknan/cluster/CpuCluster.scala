package linknan.cluster

import chisel3._
import chisel3.experimental.hierarchy.core.IsLookupable
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp, LazyRawModuleImp}
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters, TLClientNode, TLEdgeIn, TLManagerNode, TLXbar}
import linknan.cluster.core.{NanhuCoreWrapper, NoCoreWrapper}
import linknan.cluster.hub.{AlwaysOnDomain, ImsicBundle}
import linknan.cluster.hub.interconnect.ClusterDeviceBundle
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xijiang.{Node, NodeType}
import xs.utils.perf.PerfCounterUtils
import zhujiang.{ZJParametersKey, ZJRawModule}

class BlockTestIO(val params:BlockTestIOParams)(implicit p:Parameters) extends Bundle {
  val clock = Output(Clock())
  val reset = Output(AsyncReset())
  val cio = Flipped(new TLBundle(params.cioTlParams))
  val icache = Flipped(new TLBundle(params.icacheTlParams))
  val ptw = Flipped(new TLBundle(params.ptwTlParams))
  val dcache = Flipped(new TLBundle(params.dcacheTlParams))
  val mhartid = Output(UInt(p(ZJParametersKey).clusterIdBits.W))
}

case class BlockTestIOParams(
  cioTlParams:TLBundleParameters,
  icacheTlParams: TLBundleParameters,
  ptwTlParams: TLBundleParameters,
  dcacheTlParams: TLBundleParameters,
  node:Node
) extends IsLookupable

class CpuCluster(node:Node)(implicit p:Parameters) extends LazyModule {
  private val removeCore = p(LinkNanParamsKey).removeCore
  private val tile = if(removeCore) {
    LazyModule(new NoCoreWrapper(node.copy(nodeType = NodeType.RF)))
  } else {
    LazyModule(new NanhuCoreWrapper(node.copy(nodeType = NodeType.RF)))
  }

  val btIoParams = Option.when(removeCore)(tile.asInstanceOf[NoCoreWrapper].btioParams)
  lazy val module = new CpuClusterImpl
  @instantiable
  class CpuClusterImpl(implicit p:Parameters) extends LazyRawModuleImp(this) {
    @public val icn = IO(new ClusterDeviceBundle(node))
    @public val btio = if(removeCore) Some(IO(new BlockTestIO(btIoParams.get))) else None
    private val hub = Module(new AlwaysOnDomain(node))
    icn <> hub.io.icn
    hub.io.cpu <> tile.module.io
    if(removeCore) {
      btio.get <> tile.asInstanceOf[NoCoreWrapper].module.btio
    }
    PerfCounterUtils.genXmrHelper()
  }
}
