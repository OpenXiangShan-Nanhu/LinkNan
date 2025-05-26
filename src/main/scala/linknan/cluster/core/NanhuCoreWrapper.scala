package linknan.cluster.core

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import chisel3.util._
import coupledL2.tl2chi.TL2CHICoupledL2
import freechips.rocketchip.devices.debug.DebugModuleKey
import freechips.rocketchip.interrupts.{IntSourceNode, IntSourcePortSimple}
import freechips.rocketchip.tilelink.{BankBinder, TLBuffer, TLXbar}
import linknan.soc.LinkNanParamsKey
import linknan.utils.{connectByName, connectChiChn}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.bundlebridge.BundleBridgeSource
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import xiangshan._
import xijiang.Node
import xs.utils.IntBuffer
import xs.utils.cache.common.L2ParamKey
import xs.utils.debug.{HardwareAssertion, HardwareAssertionKey}
import xs.utils.sram.SramHelper
import zhujiang.HasZJParams
import zhujiang.chi.FlitHelper.{connIcn, hwaConn}
import zhujiang.chi._

class NanhuCoreWrapper(node:Node)(implicit p:Parameters) extends BaseCoreWrapper {
  //Core connections
  private val core = LazyModule(new XSCore()(p.alterPartial({
    case DebugModuleKey => Some(p(LinkNanParamsKey).debugParams)
  })))
  private val memBlock = core.memBlock.inner
  private val cacheXBar = LazyModule(new TLXbar)
  private val mmioXBar = LazyModule(new TLXbar)

  private val clintIntNode = IntSourceNode(IntSourcePortSimple(1, 1, 2))
  private val debugIntNode = IntSourceNode(IntSourcePortSimple(1, 1, 1))
  private val plicIntNode = IntSourceNode(IntSourcePortSimple(1, 2, 1))
  private val nmiIntNode = IntSourceNode(IntSourcePortSimple(1, 1, (new NonmaskableInterruptIO).elements.size))

  private val l3pfSink = memBlock.l3_pf_sender_opt.map(_.makeSink())
  private val cmoSink = memBlock.cmo_sender.map(_.makeSink())
  private val cmoSource = memBlock.cmo_reciver.map(n => BundleBridgeSource(n.genOpt.get))
  memBlock.cmo_reciver.foreach(_ := cmoSource.get)

  memBlock.clint_int_sink := IntBuffer(3, cdc = true) := clintIntNode
  memBlock.debug_int_sink := IntBuffer(3, cdc = true) := debugIntNode
  memBlock.plic_int_sink :*= IntBuffer(3, cdc = true) :*= plicIntNode
  memBlock.nmi_int_sink := IntBuffer(3, cdc = true) := nmiIntNode

  cacheXBar.node :*= TLBuffer.chainNode(1, Some(s"l1d_buffer")) :*= memBlock.dcache_port :*= memBlock.l1d_to_l2_buffer.node :*= memBlock.dcache.clientNode
  cacheXBar.node :*= TLBuffer.chainNode(1, Some(s"ptw_buffer")) :*= memBlock.ptw_to_l2_buffer.node
  cacheXBar.node :*= TLBuffer.chainNode(1, Some(s"l1i_buffer")) :*= memBlock.frontendBridge.icache_node

  mmioXBar.node :*= TLBuffer.chainNode(1, Some(s"mem_uc_buffer")) :*= memBlock.uncache.clientNode
  mmioXBar.node :*= TLBuffer.chainNode(1, Some(s"frd_uc_buffer")) :*= memBlock.frontendBridge.instr_uncache_node

  //l2 Connections
  private val l2cache = LazyModule(new TL2CHICoupledL2)
  private val tpMetaSinkNode = l2cache.tpmeta_source_node.map(_.makeSink())
  private val tpMetaSourceNode = l2cache.tpmeta_sink_node.map(n => BundleBridgeSource(n.genOpt.get))
  l2cache.tpmeta_sink_node.foreach(_ := tpMetaSourceNode.get)

  l2cache.pf_recv_node.foreach(_ := memBlock.l2_pf_sender_opt.get)
  l2cache.mmioNode :*= TLBuffer() :*= mmioXBar.node
  l2cache.managerNode :=*
    TLBuffer.chainNode(1, Some(s"l2_bank_buffer")) :=*
    TLXbar() :=*
    BankBinder(p(XSCoreParamsKey).L2NBanks, p(L2ParamKey).blockBytes) :*=
    l2cache.node :*=
    TLBuffer() :*=
    cacheXBar.node

  lazy val module = new NanhuCoreWrapperImpl
  @instantiable
  class NanhuCoreWrapperImpl extends BaseCoreWrapperImpl(this, node) with HasZJParams {
    private val _l2 = l2cache.module
    private val _core = core.module
    _core.io.hartId := io.mhartid
    _l2.io.hartId := io.mhartid
    _core.io.msiInfo := DontCare
    _core.io.clintTime := Pipe(io.timerUpdate)
    _core.io.reset_vector := io.reset_vector
    cpuHalt := _core.io.cpu_halt
    _core.io.traceCoreInterface := DontCare
    _l2.io.pfCtrlFromCore := _core.io.l2PfCtrl
    _core.io.l2_hint := _l2.io.l2_hint
    _core.io.l2PfqBusy := false.B

    _core.io.l2_tlb_req.req.bits := DontCare
    _core.io.l2_tlb_req.req.valid := _l2.io.l2_tlb_req.req.valid
    _core.io.l2_tlb_req.resp.ready := _l2.io.l2_tlb_req.resp.ready
    _core.io.l2_tlb_req.req.bits.vaddr := _l2.io.l2_tlb_req.req.bits.vaddr
    _core.io.l2_tlb_req.req.bits.cmd := _l2.io.l2_tlb_req.req.bits.cmd
    _core.io.l2_tlb_req.req.bits.size := _l2.io.l2_tlb_req.req.bits.size
    _core.io.l2_tlb_req.req.bits.kill := _l2.io.l2_tlb_req.req.bits.kill
    _core.io.l2_tlb_req.req.bits.no_translate := _l2.io.l2_tlb_req.req.bits.no_translate
    _core.io.l2_tlb_req.req_kill := _l2.io.l2_tlb_req.req_kill

    _l2.io.l2_tlb_req.resp.valid := _core.io.l2_tlb_req.resp.valid
    _l2.io.l2_tlb_req.req.ready := _core.io.l2_tlb_req.req.ready
    _l2.io.l2_tlb_req.resp.bits.paddr.head := _core.io.l2_tlb_req.resp.bits.paddr.head
    _l2.io.l2_tlb_req.resp.bits.pbmt := _core.io.l2_tlb_req.resp.bits.pbmt.head
    _l2.io.l2_tlb_req.resp.bits.miss := _core.io.l2_tlb_req.resp.bits.miss
    _l2.io.l2_tlb_req.resp.bits.excp.head.gpf := _core.io.l2_tlb_req.resp.bits.excp.head.gpf
    _l2.io.l2_tlb_req.resp.bits.excp.head.pf := _core.io.l2_tlb_req.resp.bits.excp.head.pf
    _l2.io.l2_tlb_req.resp.bits.excp.head.af := _core.io.l2_tlb_req.resp.bits.excp.head.af
    _l2.io.l2_tlb_req.pmp_resp.ld := _core.io.l2_pmp_resp.ld
    _l2.io.l2_tlb_req.pmp_resp.st := _core.io.l2_pmp_resp.st
    _l2.io.l2_tlb_req.pmp_resp.instr := _core.io.l2_pmp_resp.instr
    _l2.io.l2_tlb_req.pmp_resp.mmio := _core.io.l2_pmp_resp.mmio
    _l2.io.l2_tlb_req.pmp_resp.atomic := _core.io.l2_pmp_resp.atomic

    _core.io.perfEvents := DontCare
    _core.io.perfEvents.zip(_l2.io_perf).foreach({case(a, b) => a := b})
    private val allPerfEvents = _l2.getPerfEvents
    for (((name, inc), i) <- allPerfEvents.zipWithIndex) {
      println("L2 Cache perfEvents Set", name, inc, i)
    }

    _l2.io_nodeID := 0.U(7.W)
    _l2.io.l2Flush.foreach(_ := false.B)
    _l2.io.dft.func.foreach(_ := io.dft.func)
    _l2.io.dft.reset.foreach(_ := io.dft.reset)
    _l2.io.ramctl := io.ramctl

    reset_state := (_core.io.resetInFrontend || implicitReset.asBool).asAsyncReset

    _l2.io.debugTopDown.robTrueCommit := _core.io.debugTopDown.robTrueCommit
    _l2.io.debugTopDown.robHeadPaddr := _core.io.debugTopDown.robHeadPaddr
    _core.io.debugTopDown.l2MissMatch := _l2.io.debugTopDown.l2MissMatch
    _core.io.debugTopDown.l3MissMatch := false.B
    _core.io.dft.func.foreach(_ := io.dft.func)
    _core.io.dft.reset.foreach(_ := io.dft.reset)
    _core.io.ramctl := io.ramctl

    clintIntNode.out.head._1(0) := io.msip
    clintIntNode.out.head._1(1) := io.mtip
    plicIntNode.out.head._1(0) := io.meip
    plicIntNode.out.last._1(0) := io.seip
    debugIntNode.out.head._1(0) := io.dbip

    cmoSink.foreach(_.in.head._1.ready := false.B)
    cmoSource.foreach(_.out.head._1 := DontCare)
    tpMetaSinkNode.foreach(_.in.head._1.ready := false.B)
    tpMetaSourceNode.foreach(_.out.head._1 := DontCare)

    l2cache.module.io_chi match {
      case l2_chi: coupledL2.tl2chi.DecoupledPortIO =>
        connectByName(txreq, l2_chi.tx.req)
        txreq.bits.SnoopMe := l2_chi.tx.req.bits.snoopMe
        txreq.bits.QoS := 0.U
        connectByName(txrsp, l2_chi.tx.rsp)
        connectByName(txdat, l2_chi.tx.dat)
        connectByName(l2_chi.rx.snp, rxsnp)
        connectByName(l2_chi.rx.rsp, rxrsp)
        connectByName(l2_chi.rx.dat, rxdat)
      case _ => require(requirement = false, "Credit-Grant CHI is not supported!")
    }
    private val assertionNode = HardwareAssertion.placePipe(Int.MaxValue, moduleTop = true).map(_.head)
    HardwareAssertion.release(assertionNode, "hwa", "core")
    assertionNode.foreach(_.hassert.bus.get.ready := true.B)
    if(p(HardwareAssertionKey).enable) {
      val dbgBd = WireInit(0.U.asTypeOf(Decoupled(new RingFlit(debugFlitBits))))
      if(assertionNode.isDefined) {
        dontTouch(assertionNode.get.hassert)
        hwaConn(dbgBd, assertionNode.get.hassert)
      }
      connIcn(pdc.io.icn.rx.debug.get, dbgBd)
    }
  }
}
