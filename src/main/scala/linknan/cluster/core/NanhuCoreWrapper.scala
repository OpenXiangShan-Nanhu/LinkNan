package linknan.cluster.core

import SimpleL2.Configs.L2ParamKey
import chisel3._
import chisel3.experimental.hierarchy.instantiable
import darecreek.exu.vfu.{VFuParameters, VFuParamsKey}
import freechips.rocketchip.interrupts.{IntSourceNode, IntSourcePortSimple}
import freechips.rocketchip.tilelink.{TLBuffer, TLXbar}
import linknan.generator.DcacheKey
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.LazyModule
import xs.utils.IntBuffer
import xiangshan.{XSCore, XSCoreParameters, XSCoreParamsKey}
import xs.utils.tl.{TLUserKey, TLUserParams}

class NanhuCoreWrapper(implicit p:Parameters) extends BaseCoreWrapper {
  private val dcacheParams = p(DcacheKey)
  private val core = LazyModule(new XSCore()(p.alterPartial({
    case XSCoreParamsKey => XSCoreParameters(dcacheParametersOpt = Some(p(DcacheKey)), L2NBanks = p(L2ParamKey).nrSlice)
    case TLUserKey => TLUserParams(aliasBits = dcacheParams.aliasBitsOpt.getOrElse(0))
    case VFuParamsKey => VFuParameters()
  })))
  private val coreXBar = LazyModule(new TLXbar)
  private val clintIntBuf = LazyModule(new IntBuffer)
  private val plicIntBuf = LazyModule(new IntBuffer)
  private val debugIntBuf = LazyModule(new IntBuffer)
  private val clintIntSrc = IntSourceNode(IntSourcePortSimple(2, 1))
  private val plicIntSrc = IntSourceNode(IntSourcePortSimple(1, 2))
  private val debugIntSrc = IntSourceNode(IntSourcePortSimple(1, 1))
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"l1d_buffer")) :*= core.exuBlock.memoryBlock.dcache.clientNode
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"ptw_buffer")) :*= core.ptw_to_l2_buffer.node
  coreXBar.node :*= TLBuffer.chainNode(1, Some(s"l1i_buffer")) :*= core.frontend.icache.clientNode
  core.clint_int_sink :*= clintIntBuf.node :*= clintIntSrc
  core.plic_int_sink :*= plicIntBuf.node :*= plicIntSrc
  core.debug_int_sink :*= debugIntBuf.node :*= debugIntSrc
  cioNode :*= TLBuffer.chainNode(1, Some(s"uncache_buffer")) :*= core.uncacheBuffer.node
  l2Node :*= TLBuffer.chainNode(1, Some(s"l2_buffer")) :*= coreXBar.node
  lazy val module = new NanhuCoreWrapperImpl

  @instantiable
  class NanhuCoreWrapperImpl extends BaseCoreWrapperImpl(this) {
    cpuHalt := core.module.io.cpu_halt
    core.module.io.hartId := io.mhartid
    core.module.io.reset_vector := io.reset_vector
    core.module.io.perfEvents := DontCare
    io.icacheErr := core.module.io.l1iErr
    io.dcacheErr := core.module.io.l1dErr
    core.module.io.dfx_reset := io.dft.reset
    core.module.dft.foreach(_ := io.dft.func)
    clintIntSrc.out.head._1(0) := io.msip
    clintIntSrc.out.head._1(1) := io.mtip
    plicIntSrc.out.head._1(0) := io.meip
    plicIntSrc.out.last._1(0) := io.seip
    debugIntSrc.out.head._1(0) := io.dbip
  }
}
