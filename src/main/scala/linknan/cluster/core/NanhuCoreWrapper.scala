package linknan.cluster.core

import chisel3._
import chisel3.experimental.hierarchy.instantiable
import freechips.rocketchip.interrupts.{IntSourceNode, IntSourcePortSimple}
import freechips.rocketchip.tilelink.{TLBuffer, TLXbar}
import linknan.generator.DcacheKey
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import org.chipsalliance.diplomacy.bundlebridge._
import xs.utils.IntBuffer
import xiangshan.{XLen, PMParameKey, PMParameters, XSCore, XSCoreParameters, XSCoreParamsKey, NonmaskableInterruptIO}
import xs.utils.tl.{TLUserKey, TLUserParams}
import xs.utils.perf.{DebugOptionsKey, PerfCounterOptions, PerfCounterOptionsKey, LogUtilsOptionsKey, LogUtilsOptions}


class NanhuCoreWrapper(implicit p:Parameters) extends BaseCoreWrapper {

  private val dcacheParams = p(DcacheKey)
  private val debugParams = p(DebugOptionsKey)
  private val core = LazyModule(new XSCore()(p.alterPartial({
    case XLen => 64
    case XSCoreParamsKey => XSCoreParameters()
    case TLUserKey => TLUserParams(aliasBits = dcacheParams.aliasBitsOpt.getOrElse(0))
    case PMParameKey => PMParameters()
    case PerfCounterOptionsKey => PerfCounterOptions(!debugParams.FPGAPlatform, false, 0)
    case LogUtilsOptionsKey => LogUtilsOptions(debugParams.EnableDebug, debugParams.EnablePerfDebug, debugParams.FPGAPlatform)
  })))
  private val coreXBar = LazyModule(new TLXbar)
  private val mmioXbar = LazyModule(new TLXbar)
  private val clintIntBuf = LazyModule(new IntBuffer)
  private val plicIntBuf = LazyModule(new IntBuffer)
  private val debugIntBuf = LazyModule(new IntBuffer)
  private val nmiIntBuf = LazyModule(new IntBuffer)
  private val clintIntSrc = IntSourceNode(IntSourcePortSimple(2, 1))
  private val plicIntSrc = IntSourceNode(IntSourcePortSimple(1, 2))
  private val debugIntSrc = IntSourceNode(IntSourcePortSimple(1, 1))
  private val nmiIntNode = IntSourceNode(IntSourcePortSimple(1, 1, (new NonmaskableInterruptIO).elements.size))
  private val l2_pf_recv_node: BundleBridgeSink[coupledL2.PrefetchRecv] = BundleBridgeSink(Some(() => new coupledL2.PrefetchRecv))
  private val l3_pf_recv_node: BundleBridgeSink[huancun.PrefetchRecv] = BundleBridgeSink(Some(() => new huancun.PrefetchRecv))

  core.memBlock.inner.clint_int_sink :*= clintIntBuf.node :*= clintIntSrc
  core.memBlock.inner.plic_int_sink :*= plicIntBuf.node :*= plicIntSrc
  core.memBlock.inner.debug_int_sink :*= debugIntBuf.node :*= debugIntSrc
  core.memBlock.inner.nmi_int_sink :*= nmiIntBuf.node :*= nmiIntNode
  l2_pf_recv_node :*= core.memBlock.inner.l2_pf_sender_opt.get
  l3_pf_recv_node :*= core.memBlock.inner.l3_pf_sender_opt.get
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"l1d_buffer")) :*= core.memBlock.inner.dcache.clientNode
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"ptw_buffer")) :*= core.memBlock.inner.ptw_to_l2_buffer.node
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"l1i_buffer")) :*= core.memBlock.inner.frontendBridge.icache_node
  mmioXbar.node :*= TLBuffer.chainNode(1, Some(s"inst_uncache_buffer")) :*= core.memBlock.inner.frontendBridge.instr_uncache_node
  mmioXbar.node :*= TLBuffer.chainNode(1, Some(s"data_uncache_buffer")) :*= core.memBlock.inner.uncache.clientNode
  cioNode :*= TLBuffer.chainNode(1, Some(s"uncache_buffer")) :*= mmioXbar.node
  l2Node :*= TLBuffer.chainNode(1, Some(s"l2_buffer")) :*= coreXBar.node

  lazy val module = new NanhuCoreWrapperImpl

  @instantiable
  class NanhuCoreWrapperImpl extends BaseCoreWrapperImpl(this) {
    core.module.io := DontCare

    cpuHalt := core.module.io.cpu_halt
    core.module.io.hartId := io.mhartid
    core.module.io.reset_vector := io.reset_vector
    core.module.io.perfEvents := DontCare
    io.icacheErr.ecc_error := core.module.io.beu_errors.icache.ecc_error
    io.dcacheErr.ecc_error := core.module.io.beu_errors.dcache.ecc_error
    core.module.dft_reset := io.dft.reset
    core.module.dft.foreach(_ := io.dft.func)
    clintIntSrc.out.head._1(0) := io.msip
    clintIntSrc.out.head._1(1) := io.mtip
    plicIntSrc.out.head._1(0) := io.meip
    plicIntSrc.out.last._1(0) := io.seip
    debugIntSrc.out.head._1(0) := io.dbip
    nmiIntNode.out.head._1(0) := false.B
    dontTouch(nmiIntNode.out.head._1(0))

  }
}
