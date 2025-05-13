package linknan.soc

import aia.{APLICParams, IMSICParams}
import chisel3._
import chisel3.experimental.{SourceInfo, SourceLine}
import chisel3.util._
import freechips.rocketchip.devices.debug.DebugModuleParams
import org.chipsalliance.cde.config.{Field, Parameters}
import xijiang.NodeType
import zhujiang.ZJParametersKey

case object LinkNanParamsKey extends Field[LinkNanParams]

case class LinkNanParams(
  removeCore: Boolean = false,
  keepL1c: Boolean = false,
  prefix:String = "",
  iodChipId:Int = 6,
  nrExtIntr: Int = 64,
  remapBase:Long = 0xE0_0000_0000L,
  remapMask: Long = 0xE0_1FFF_FFFFL,
  memBase:Long = 0x8000_0000L,
  memSizeInMiB: Long = 128,
  imiscSgBase: Long = 0x0000_0000L,
  imsicMBase: Long = 0x0080_0000L,
  debugBase: Long = 0x0800_0000L,
  plicBase: Long = 0x0400_0000L,
  mswiBase: Long = 0x0100_0000L,
  sswiBase: Long = 0x0100_4000L,
  refTimerBase: Long = 0x0100_8000L,
  rtcFreq: Long = 1_000_000
) {
  lazy val remapBaseMaskBits = Seq.tabulate(64)(i => (remapMask >> i) & 0x1L).sum.toInt
  lazy val finalSgBase = remapBase + imiscSgBase
  lazy val finalMBase = remapBase + imsicMBase
  lazy val aplicParams = APLICParams(
    aplicIntSrcWidth = log2Ceil(nrExtIntr),
    imsicIntSrcWidth = log2Ceil(nrExtIntr) + 1,
    baseAddr = 0x3805_0000L,
    membersNum = 1,
    mBaseAddr = finalMBase,
    sgBaseAddr = finalSgBase,
    groupsNum = 1,
    geilen = 1
  )
  lazy val imiscParams = IMSICParams(
    imsicIntSrcWidth = aplicParams.imsicIntSrcWidth,
    mAddr = 0x8000L,
    sgAddr = 0x0000L,
    geilen = aplicParams.geilen
  )
  lazy val debugParams = DebugModuleParams(
    nAbstractDataWords = 2,
    maxSupportedSBAccess = 64,
    hasBusMaster = true,
    baseAddress = BigInt(debugBase),
    nScratch = 2,
    crossingHasSafeReset = false
  )
  lazy val memSizeInB = memSizeInMiB * 1024 * 1024
  def legalPeriSets(core: Int, cpuSpaceSize:Long): Seq[(Long, Int)] = {
    val coreBase = core * cpuSpaceSize
    val coreDevAddrSets = Seq.tabulate(core){ i => Seq(
      (i * coreBase + 0x0000, 0x08), // CBAR
      (i * coreBase + 0x1000, 0x10), // PPU
      (i * coreBase + 0x2000, 0x18), // DACLINT
    )}
    coreDevAddrSets.reduce(_ ++ _) ++ Seq(
      (plicBase, 0x400_0000),
      (debugBase, 0x1000),
      (0x1000_0000L, 0x1000_0000),
      (0x4060_0000L, 0x10),
      (0x4007_0000L, 0x1_0000),
      (0x4008_0000L, 0x1_0000)
    )
  }
  def checkPeriAddr(addr:UInt)(implicit p:Parameters, s:SourceInfo): Unit = {
    val core = p(ZJParametersKey).island.filter(_.nodeType == NodeType.CC).map(_.cpuNum).sum
    val hit = Cat(legalPeriSets(core, 1 << p(ZJParametersKey).cpuSpaceBits).map(elm =>  elm._1.U <= addr && addr < (elm._1 + elm._2).U)).orR
    val desc = cf"Illegal peri addr 0x${addr}%x"
    val descStr = s match {
      case SourceLine(filename, line, col) =>
        val fn = filename.replaceAll("\\\\", "/")
        cf"$fn:$line:$col: " + desc
      case _ => desc
    }
    assert(hit, descStr)(s)
  }
}
