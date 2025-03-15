package linknan.generator

import SimpleL2.Configs.{L2Param, L2ParamKey}
import linknan.soc.{LinkNanParams, LinkNanParamsKey}
import org.chipsalliance.cde.config.{Config, _}
import xiangshan.cache.DCacheParameters
import xijiang.{NodeParam, NodeType}
import xs.utils.perf.{DebugOptions, DebugOptionsKey}
import zhujiang.{ZJParameters, ZJParametersKey}

case object TestIoOptionsKey extends Field[TestIoOptions]
case object DcacheKey extends Field[DCacheParameters]
case object MiscKey extends Field[MiscOpts]

case class TestIoOptions(
  removeCore: Boolean = false,
  removeCsu: Boolean = false,
  keepImsic:Boolean = true,
) {
  val doBlockTest = removeCore || removeCsu
  val hasCsu = !removeCsu
}

case class MiscOpts(
  prefix:String = "",
  random:Boolean = false
)

class BaseConfig(core:String) extends Config((site, here, up) => {
  case DebugOptionsKey => DebugOptions()
  case MiscKey => MiscOpts("", core == "boom")
  case TestIoOptionsKey => TestIoOptions()
  case LinkNanParamsKey => LinkNanParams()
  case DcacheKey => DCacheParameters()
  case L2ParamKey => L2Param(useDiplomacy = true)
})

class FullNocConfig(core:String, socket:String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main"),
      NodeParam(nodeType = NodeType.P),

      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.S, bankId = 0),
      NodeParam(nodeType = NodeType.S, bankId = 1),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class ReducedNocConfig(core:String, socket:String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 2, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main"),
      NodeParam(nodeType = NodeType.P),

      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.S, bankId = 0),
      NodeParam(nodeType = NodeType.S, bankId = 1),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class MinimalNocConfig(core:String, socket:String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 1, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main"),
      NodeParam(nodeType = NodeType.P),

      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.S, bankId = 0),
      NodeParam(nodeType = NodeType.S, bankId = 1),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class LLCConfig(sizeInMiB:Int = 8, ways:Int = 16, sfWays:Int = 16) extends Config((site, here, up) => {
  case ZJParametersKey => up(ZJParametersKey).copy(
    cacheSizeInB = sizeInMiB * 1024 * 1024,
    cacheWays = ways,
    snoopFilterWays = sfWays
  )
})

class L2Config(sizeInKiB:Int = 512, ways:Int = 8, slices:Int = 2) extends Config((site, here, up) => {
  case L2ParamKey => up(L2ParamKey).copy(
    ways = ways,
    nrSlice = slices,
    sets = sizeInKiB * 1024 / ways / slices / up(L2ParamKey).blockBytes
  )
  case ZJParametersKey => up(ZJParametersKey).copy(
      clusterCacheSizeInB = sizeInKiB * 1024
    )
})

class L1DConfig(sizeInKiB:Int = 64, ways:Int = 4) extends Config((site, here, up) => {
  case DcacheKey =>
    up(DcacheKey).copy(
      nSets = sizeInKiB * 1024 / ways / 64,
      nWays = ways,
      tagECC = Some("secded"),
      dataECC = Some("secded"),
      replacer = Some("setplru"),
      nMissEntries = 16,
      nProbeEntries = 8,
      nReleaseEntries = 18
    )
})

class FullConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config ++ new LLCConfig ++ new FullNocConfig(core, socket) ++ new BaseConfig(core)
)

class ReducedConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config(512, 8) ++ new LLCConfig(4, 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)

class MinimalConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(2, 8) ++ new MinimalNocConfig(core, socket) ++ new BaseConfig(core)
)

class SpecConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config ++ new LLCConfig ++ new MinimalNocConfig(core, socket) ++ new BaseConfig(core)
)

class FpgaConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(8, 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)

class BtestConfig(core:String, socket:String) extends Config(
  new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(1, 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)