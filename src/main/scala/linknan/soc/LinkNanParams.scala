package linknan.soc

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
  val remapBaseMaskBits = Seq.tabulate(64)(i => (remapMask >> i) & 0x1L).sum.toInt
  val finalSgBase = remapBase + imiscSgBase
  val finalMBase = remapBase + imsicMBase
}
