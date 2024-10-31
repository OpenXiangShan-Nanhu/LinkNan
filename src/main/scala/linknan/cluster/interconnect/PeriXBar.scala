package linknan.cluster.interconnect

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJParametersKey
import zhujiang.tilelink.{BaseTLULPeripheral, BaseTLULXbar, TLULBundle, TilelinkParams}

case class ClusterPeriParams(
  name: String,
  addrSet: Seq[(Int, Int)]
) {
  def matcher(slvAddrBits:Int)(addr:UInt):Bool = {
    val devAddr = addr(slvAddrBits - 1, 0)
    Cat(addrSet.map(as => as._1.U <= devAddr && devAddr < as._2.U)).orR
  }
}

class PeriXBar(tlParams: Seq[TilelinkParams], periParams: Seq[ClusterPeriParams])(implicit p: Parameters) extends BaseTLULXbar {
  private val coreIdBits = clusterIdBits - nodeAidBits
  private val cpuSpaceBits = p(ZJParametersKey).cpuSpaceBits
  private val devSpaceBits = p(ZJParametersKey).cpuDevSpaceBits
  private val mstAddrBits = cpuSpaceBits + coreIdBits
  val mstParams = tlParams.map(_.copy(addrBits = mstAddrBits))
  val slvAddrBits = devSpaceBits

  val slvMatchersSeq = periParams.map(_.matcher(slvAddrBits))
  initialize()
}
