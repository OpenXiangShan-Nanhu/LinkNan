package linknan.cluster.hub.interconnect

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJParametersKey
import zhujiang.tilelink.{BaseTLULPeripheral, BaseTLULXbar, TLULBundle, TilelinkParams}

case class ClusterPeriParams(
  name: String,
  addrSet: Seq[(Int, Int)],
  hart: Option[Int]
) {
  def matcher(slvAddrBits:Int, cores:Vec[UInt])(addr:UInt):Bool = {
    val cpuAddr = addr(cores.head.getWidth + slvAddrBits - 1, slvAddrBits)
    val devAddr = addr(slvAddrBits - 1, 0)
    val cpuMatch = if(hart.isDefined) cores(hart.get) === cpuAddr else true.B
    val devMatch = addrSet.map(as => as._1.U <= devAddr && devAddr < as._2.U).reduce(_ || _)
    cpuMatch && devMatch
  }
}

class PeriXBar(tlParams: Seq[TilelinkParams], periParams: Seq[ClusterPeriParams], coreNum:Int)(implicit p: Parameters) extends BaseTLULXbar {
  private val coreIdBits = cpuIdBits
  private val cpuSpaceBits = p(ZJParametersKey).cpuSpaceBits
  private val mstAddrBits = cpuSpaceBits + coreIdBits
  val mstParams = tlParams.map(_.copy(addrBits = mstAddrBits))
  val slvAddrBits = cpuSpaceBits

  val cores = IO(Input(Vec(coreNum, UInt(coreIdBits.W))))
  val slvMatchersSeq = periParams.map(_.matcher(slvAddrBits, cores))
  initialize()
}
