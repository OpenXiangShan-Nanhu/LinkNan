package linknan.soc

import aia.{APLICParams, IMSICParams}
import chisel3.util.log2Ceil
import freechips.rocketchip.devices.debug.DebugModuleParams
import org.chipsalliance.cde.config.Field

case object LinkNanParamsKey extends Field[LinkNanParams]

case class LinkNanParams(
  removeCore: Boolean = false,
  prefix:String = "",
  random:Boolean = false,
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
}
