package linknan.cluster

import chisel3._
import chisel3.experimental.hierarchy.core.IsLookupable
import chisel3.experimental.hierarchy.{Definition, Instance, instantiable, public}
import darecreek.exu.vfu.{VFuParameters, VFuParamsKey}
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyModuleImp}
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters, TLClientNode, TLEdgeIn, TLManagerNode, TLXbar}
import linknan.cluster.core.{BoomV4CoreWrapper, NanhuCoreWrapper}
import linknan.cluster.hub.{AlwaysOnDomain, ImsicBundle}
import linknan.cluster.hub.interconnect.ClusterDeviceBundle
import linknan.generator.TestIoOptionsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import xijiang.{Node, NodeType}
import xijiang.router.base.DeviceIcnBundle
import xs.utils.tl.{TLUserKey, TLUserParams}
import zhujiang.{ZJParametersKey, ZJRawModule}

class BlockTestIO(val params:BlockTestIOParams)(implicit p:Parameters) extends Bundle {
  val clock = Output(Clock())
  val reset = Output(AsyncReset())
  val cio = Flipped(new TLBundle(params.ioParams))
  val mhartid = Output(UInt(p(ZJParametersKey).clusterIdBits.W))
  val imsic = if(p(TestIoOptionsKey).keepImsic) Some(new ImsicBundle) else None
  val l2 = if(p(TestIoOptionsKey).removeCsu) None else Some(Flipped(new TLBundle(params.l2Params)))
  val icn = if(p(TestIoOptionsKey).removeCsu) Some(Flipped(new DeviceIcnBundle(params.node))) else None
}

class TestCioXBar(cioEdge: TLEdgeIn, cpuNum:Int)(implicit p:Parameters) extends LazyModule {
  private val cioInNodes = Seq.fill(cpuNum)(TLClientNode(Seq(cioEdge.master)))
  private val cioXbar = LazyModule(new TLXbar)
  val cioOutNode = TLManagerNode(Seq(cioEdge.slave))
  cioInNodes.foreach(n => cioXbar.node :*= n)
  cioOutNode :*= cioXbar.node
  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle{
      val cio = Vec(cpuNum, Flipped(new TLBundle(cioEdge.bundle)))
      val hub = new TLBundle(cioOutNode.edges.in.head.bundle)
    })
    cioInNodes.zip(io.cio).foreach({case(a, b) => b <> a.out.head._1})
    io.hub <> cioOutNode.in.head._1
  }
}

case class BlockTestIOParams(ioParams:TLBundleParameters, l2Params: TLBundleParameters, node:Node) extends IsLookupable

@instantiable
class CpuCluster(node:Node)(implicit p:Parameters) extends ZJRawModule {
  private val removeCsu = p(TestIoOptionsKey).removeCsu
  private val removeCore = p(TestIoOptionsKey).removeCore || p(TestIoOptionsKey).removeCsu

  private val coreGen = node.attr match {
    case "boom" => LazyModule(new BoomV4CoreWrapper)
    case _ => LazyModule(new NanhuCoreWrapper)
  }
  private val coreDef = if(!removeCore) Some(Definition(coreGen.module)) else None
  private val coreSeq = if(!removeCore) Some(Seq.fill(node.cpuNum)(Instance(coreDef.get))) else None
  coreSeq.foreach(_.zipWithIndex.foreach({case(c, i) => c.suggestName(s"core_$i")}))
  private val cioEdge = coreGen.cioNode.edges.in.head
  private val cl2Edge = coreGen.l2Node.edges.in.head

  private val csu = if(removeCsu) None else Some(LazyModule(new ClusterSharedUnit(cioEdge, cl2Edge, node.copy(nodeType = NodeType.RF))))
  private val cioXbar = if(removeCsu) Some(LazyModule(new TestCioXBar(cioEdge, node.cpuNum))) else None
  private val finalCioEdge = if(csu.isDefined) csu.get.cioOutNode.edges.in.head else cioXbar.get.cioOutNode.edges.in.head
  private val hub = Module(new AlwaysOnDomain(node, finalCioEdge.bundle))
  private val _cioXbar = if(removeCsu) Some(withClockAndReset(hub.io.cluster.csu.clock, hub.io.cluster.csu.reset){ Module(cioXbar.get.module) }) else None
  private val _csu = csu.map(c => Module(c.module))

  private val btNode = if(p(TestIoOptionsKey).keepImsic) hub.io.cluster.csu.l2cache.node.copy(splitFlit = false) else hub.io.cluster.csu.l2cache.node
  @public val coreIoParams = BlockTestIOParams(cioEdge.bundle, cl2Edge.bundle, btNode)
  @public val icn = IO(new ClusterDeviceBundle(node))
  @public val core = if(removeCore) Some(IO(Vec(node.cpuNum, new BlockTestIO(coreIoParams)))) else None
  dontTouch(icn)

  icn <> hub.io.icn
  if(removeCsu) {
    hub.io.cluster := DontCare
    hub.io.cluster.csu.pchn.accept := withClock(icn.osc_clock) { RegNext(hub.io.cluster.csu.pchn.req) }
    hub.io.cluster.csu.pwrEnAck := withClock(icn.osc_clock) { RegNext(hub.io.cluster.csu.pwrEnReq) }
    hub.io.cluster.cpu.foreach(c =>{
      c.pchn.accept := withClock(icn.osc_clock) { RegNext(c.pchn.req) }
      c.pcsm.pwrResp := withClock(icn.osc_clock) { RegNext(c.pcsm.pwrReq) }
    })
    hub.io.cluster.csu.cio <> _cioXbar.get.io.hub
  } else {
    hub.io.cluster <> _csu.get.io.hub
  }

  for(i <- 0 until node.cpuNum) {
    if(removeCsu) {
      core.get(i).clock := hub.io.cluster.csu.clock
      core.get(i).reset := hub.io.cluster.csu.reset
      _cioXbar.get.io.cio(i) <> core.get(i).cio
      core.get(i).mhartid := hub.io.cluster.cpu(i).mhartid
      core.get(i).icn.get <> hub.io.cluster.csu.l2cache
      if(core.get(i).imsic.isDefined) {
        core.get(i).imsic.get <> hub.io.cluster.cpu(i).imsic
      } else {
        hub.io.cluster.cpu(i).imsic := DontCare
      }
    } else if(removeCore) {
      _csu.get.io.core(i) := DontCare
      _csu.get.io.core(i).pchn.accept := withClock(icn.osc_clock) { RegNext(_csu.get.io.core(i).pchn.req) }
      _csu.get.io.core(i).pwrEnAck := withClock(icn.osc_clock) { RegNext(_csu.get.io.core(i).pwrEnReq) }
      core.get(i).reset := _csu.get.io.core(i).reset
      core.get(i).clock := _csu.get.io.core(i).clock
      _csu.get.io.core(i).cio <> core.get(i).cio
      _csu.get.io.core(i).l2 <> core.get(i).l2.get
      core.get(i).mhartid := _csu.get.io.core(i).mhartid
      if(core.get(i).imsic.isDefined) {
        core.get(i).imsic.get <> _csu.get.io.core(i).imsic
      } else {
        _csu.get.io.core(i).imsic.fromCpu := DontCare
      }
    } else {
      _csu.get.io.core(i) <> coreSeq.get(i).io
    }
  }
}