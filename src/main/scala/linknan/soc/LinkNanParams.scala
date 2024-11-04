package linknan.soc

import aia.{APLICParams, IMSICParams}
import chisel3.util.log2Ceil
import org.chipsalliance.cde.config.Field

case object LinkNanParamsKey extends Field[LinkNanParams]

case class LinkNanParams(
  iodChipId:Int = 6,
  nrExtIntr: Int = 64,
  remapBase:Long = 0xE0_0000_0000L,
  remapMask: Long = 0xE0_1FFF_FFFFL,
  imiscSgBase: Int = 0x0000_0000,
  imsicMBase: Int = 0x0080_0000
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
}
