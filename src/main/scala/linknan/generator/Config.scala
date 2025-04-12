package linknan.generator

import coupledL2.prefetch.{BOPParameters, PrefetchReceiverParams}
import coupledL2.{L2Param, L2ParamKey}
import linknan.soc.{LinkNanParams, LinkNanParamsKey}
import org.chipsalliance.cde.config.{Config, _}
import xiangshan.{PMParameKey, PMParameters, XSCoreParameters, XSCoreParamsKey}
import xiangshan.cache.DCacheParameters
import xiangshan.frontend.icache.ICacheParameters
import xijiang.{NodeParam, NodeType}
import xs.utils.debug.{HardwareAssertionKey, HwaParams}
import xs.utils.perf.{DebugOptions, DebugOptionsKey, LogUtilsOptions, LogUtilsOptionsKey, PerfCounterOptions, PerfCounterOptionsKey, XSPerfLevel}
import zhujiang.{ZJParameters, ZJParametersKey}

class BaseConfig(core: String) extends Config((site, here, up) => {
  case HardwareAssertionKey => HwaParams()
  case DebugOptionsKey => DebugOptions()
  case L2ParamKey => L2Param()
  case XSCoreParamsKey => XSCoreParameters()
  case PMParameKey => PMParameters()
  case LogUtilsOptionsKey => LogUtilsOptions(false, true, false)
  case PerfCounterOptionsKey => PerfCounterOptions(true, false, XSPerfLevel.VERBOSE, 0)
  case LinkNanParamsKey => LinkNanParams(random = core == "boom")
})

object AddrConfig {
  // interleaving granularity: 1KiB
  val mem0 = Seq(
    (0x0000_8000_0000L, 0x000F_8000_0400L),
    (0x0001_0000_0000L, 0x000F_0000_0400L),
    (0x0002_0000_0000L, 0x000F_0000_0400L),
    (0x0003_0000_0000L, 0x000F_0000_0400L),
    (0x0004_0000_0000L, 0x000F_0000_0400L),
  )
  val mem1 = Seq(
    (0x0000_8000_0400L, 0x000F_8000_0400L),
    (0x0001_0000_0400L, 0x000F_0000_0400L),
    (0x0002_0000_0400L, 0x000F_0000_0400L),
    (0x0003_0000_0400L, 0x000F_0000_0400L),
    (0x0004_0000_0400L, 0x000F_0000_0400L),
  )
  val mem2 = Seq(
    (0x0005_0000_0000L, 0x000F_0000_0000L),
    (0x0006_0000_0000L, 0x000F_0000_0000L),
    (0x0007_0000_0000L, 0x000F_0000_0000L),
    (0x0008_0000_0000L, 0x000F_0000_0000L),
    (0x0009_0000_0000L, 0x000F_0000_0000L),
    (0x000A_0000_0000L, 0x000F_0000_0000L),
    (0x000B_0000_0000L, 0x000F_0000_0000L),
    (0x000C_0000_0000L, 0x000F_0000_0000L),
  )
}

class FullNocConfig(core: String, socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),
      NodeParam(nodeType = NodeType.M),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem2, outstanding = 32, attr = "soc_mem"),

      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem1, outstanding = 32, attr = "loc_mem_1"),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem0, outstanding = 32, attr = "loc_mem_0"),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class ReducedNocConfig(core: String, socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),
      NodeParam(nodeType = NodeType.M),

      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem1, outstanding = 32),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem0, outstanding = 32),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class MinimalNocConfig(core: String, socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 1, outstanding = 8, attr = core, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),

      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.S,  bankId = 0, outstanding = 32),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.M),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class LLCConfig(sizeInB: Int = 8 * 1024 * 1024, ways: Int = 16, sfWays: Int = 16, outstanding: Int = 64 * 4) extends Config((site, here, up) => {
  case ZJParametersKey => up(ZJParametersKey).copy(
    cacheSizeInB = sizeInB,
    cacheWays = ways,
    snoopFilterWays = sfWays,
    hnxOutstanding = outstanding,
  )
})

class L2Config(sizeInKiB: Int = 512, ways: Int = 8, slices: Int = 2) extends Config((site, here, up) => {
  case L2ParamKey =>
    val core = up(XSCoreParamsKey)
    up(L2ParamKey).copy(
      tagECC = Some("secded"),
      dataECC = Some("secded"),
      enableTagECC = true,
      enableDataECC = true,
      dataCheck = None,
      ways = ways,
      sets = sizeInKiB * 1024 / ways / slices / up(L2ParamKey).blockBytes,
      prefetch = Seq(BOPParameters()) ++ (if(core.prefetcher.nonEmpty) Seq(PrefetchReceiverParams()) else Nil),
    )
  case XSCoreParamsKey => up(XSCoreParamsKey).copy(L2NBanks = slices)
  case ZJParametersKey => up(ZJParametersKey).copy(clusterCacheSizeInB = sizeInKiB * 1024)
})

class L1DConfig(sizeInKiB: Int = 64, ways: Int = 4) extends Config((site, here, up) => {
  case XSCoreParamsKey =>
    up(XSCoreParamsKey).copy(dcacheParametersOpt = Some(DCacheParameters(
      nSets = sizeInKiB * 1024 / ways / 64,
      nWays = ways,
      tagECC = Some("none"),
      dataECC = Some("parity"),
      replacer = Some("setplru"),
      nMissEntries = 16,
      nProbeEntries = 4,
      nReleaseEntries = 4,
      nMaxPrefetchEntry = 6,
      enableDataEcc = true,
      enableTagEcc = false
    )))
})

class L1IConfig(sizeInKiB: Int = 64, ways: Int = 4) extends Config((site, here, up) => {
  case XSCoreParamsKey =>
    up(XSCoreParamsKey).copy(icacheParameters = ICacheParameters(
      nSets = sizeInKiB * 1024 / ways / 64,
      nWays = ways,
      tagECC = Some("none"),
      dataECC = Some("parity"),
      replacer = Some("setplru")
    ))
})

class FullConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config ++ new LLCConfig ++ new FullNocConfig(core, socket) ++ new BaseConfig(core)
)

class ReducedConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config(512, 8) ++ new LLCConfig(4 * 1024 * 1024 , 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)

class ExtremeConfig(core: String, socket: String) extends Config(
  // LLCConfig: sizeInB = Cacheline * ways * sets * bank (sets must more than 2)
  new L1IConfig ++ new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(64 * 2 * 2 * 2, 2, 2, 16) ++ new FullNocConfig(core, socket) ++ new BaseConfig(core)
)

class MinimalConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(2 * 1024 * 1024, 8) ++ new MinimalNocConfig(core, socket) ++ new BaseConfig(core)
)

class SpecConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config ++ new LLCConfig ++ new MinimalNocConfig(core, socket) ++ new BaseConfig(core)
)

class FpgaConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(8 * 1024 * 1024, 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)

class BtestConfig(core: String, socket: String) extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config(256, 8) ++ new LLCConfig(1 * 1024 * 1024, 8) ++ new ReducedNocConfig(core, socket) ++ new BaseConfig(core)
)