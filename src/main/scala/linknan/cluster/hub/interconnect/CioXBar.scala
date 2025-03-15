package linknan.cluster.hub.interconnect

import chisel3._
import chisel3.util.Cat
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJParametersKey
import zhujiang.chi.ReqAddrBundle
import zhujiang.tilelink.{BaseTLULXbar, TilelinkParams}

class CioXBar(val mstParams: Seq[TilelinkParams], coreNum:Int)(implicit p: Parameters) extends BaseTLULXbar {
  private val cpuSpaceBits = p(ZJParametersKey).cpuSpaceBits
  mstParams.foreach(m => require(m.addrBits == raw))
  val slvAddrBits = raw
  val misc = IO(new Bundle {
    val ci = Input(UInt(ciIdBits.W))
    val core = Input(Vec(coreNum, UInt(cpuIdBits.W)))
  })
  private def slvMatcher(local: Boolean)(addr: UInt): Bool = {
    val reqAddr = addr.asTypeOf(new ReqAddrBundle)
    val chipMatch = reqAddr.ci === misc.ci
    val tagMatch = addr(raw - ciIdBits - 1, cpuSpaceBits + cpuIdBits) === 0.U
    val coreMatch = Cat(misc.core.map(_ === addr(cpuSpaceBits + cpuIdBits - 1, cpuSpaceBits))).orR
    val matchRes = WireInit(chipMatch & tagMatch & coreMatch)
    if(local) matchRes else !matchRes
  }
  val slvMatchersSeq = Seq(slvMatcher(local = true), slvMatcher(local = false))
  initialize()
}
