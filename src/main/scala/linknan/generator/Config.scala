package linknan.generator

import coupledL2.prefetch.PrefetchReceiverParams
import linknan.soc.{LinkNanParams, LinkNanParamsKey}
import org.chipsalliance.cde.config.{Config, _}
import xiangshan.{PMParameKey, PMParameters, XSCoreParameters, XSCoreParamsKey}
import xiangshan.cache.DCacheParameters
import xiangshan.frontend.icache.ICacheParameters
import xijiang.{NodeParam, NodeType}
import xs.utils.cache.L2Param
import xs.utils.cache.common.L2ParamKey
import xs.utils.cache.prefetch.BOPParameters
import xs.utils.debug.{HardwareAssertionKey, HwaParams}
import xs.utils.perf.{DebugOptions, DebugOptionsKey, LogUtilsOptions, LogUtilsOptionsKey, PerfCounterOptions, PerfCounterOptionsKey, XSPerfLevel}
import zhujiang.{ZJParameters, ZJParametersKey}

import scala.annotation.tailrec

class BaseConfig extends Config((site, here, up) => {
  case HardwareAssertionKey => HwaParams()
  case DebugOptionsKey => DebugOptions()
  case L2ParamKey => L2Param(hasMbist = false)
  case XSCoreParamsKey => XSCoreParameters(hasMbist = false)
  case PMParameKey => PMParameters()
  case LogUtilsOptionsKey => LogUtilsOptions(enableDebug = false, enablePerf = true, fpgaPlatform = false)
  case PerfCounterOptionsKey => PerfCounterOptions(enablePerfPrint = true, enablePerfDB = false, XSPerfLevel.VERBOSE, 0)
  case LinkNanParamsKey => LinkNanParams()
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

class FullNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem2, outstanding = 32, attr = "soc"),

      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),

      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),

      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem1, outstanding = 32, attr = "loc_1"),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem0, outstanding = 32, attr = "loc_0"),
      NodeParam(nodeType = NodeType.M),

      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
    )
  )
})

class ReducedNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),

      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem1, outstanding = 32, attr = "loc_1"),
      NodeParam(nodeType = NodeType.S,  addrSets = AddrConfig.mem0, outstanding = 32, attr = "loc_0"),

      NodeParam(nodeType = NodeType.M),
      NodeParam(nodeType = NodeType.P),
    )
  )
})

class MinimalNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, cpuNum = 1, outstanding = 8, socket = socket),
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

class ExtremeNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),

      NodeParam(nodeType = NodeType.RI, attr = "main"),
      NodeParam(nodeType = NodeType.HI, defaultHni = true, attr = "main", outstanding = 32),

      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, outstanding = 8, socket = socket),

      NodeParam(nodeType = NodeType.S, outstanding = 32),
      NodeParam(nodeType = NodeType.M),
    )
  )
})

class LLCConfig(sizeInB: Int = 8 * 1024 * 1024, ways: Int = 16, sfWays: Int = 16, outstanding: Int = 64 * 4, dirBank: Int = 2) extends Config((site, here, up) => {
  case ZJParametersKey => up(ZJParametersKey).copy(
    cacheSizeInB = sizeInB,
    cacheWays = ways,
    snoopFilterWays = sfWays,
    hnxOutstanding = outstanding,
    hnxDirSRAMBank = dirBank,
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

class FullCoreConfig extends Config(
  new L1IConfig ++ new L1DConfig ++ new L2Config
)

class MinimalCoreConfig extends Config(
  new MinimalNanhuConfig ++ new L2Config(64, 8)
)

class FullL3Config extends Config(
  new LLCConfig
)

class MediumL3Config extends Config(
  new LLCConfig(4 * 1024 * 1024, 8)
)

class SmallL3Config extends Config(
  new LLCConfig(2 * 1024 * 1024, 8)
)

// use extreme l3 config require the number of HNF no more than 4 in NoC
class ExtremeL3Config extends Config(
  new LLCConfig(64 * 16, 2, 2, 16, 1)
)

object ConfigGenerater {
  private def generate(core:String, l3:String, noc:String, socket:String):Parameters = {
    println(
      s"""core:   $core
         |l3:     $l3
         |noc:    $noc
         |socket: $socket
         |""".stripMargin)
    val coreCfg = core match {
      case "full" => new FullCoreConfig
      case "minimal" => new MinimalCoreConfig
      case _ =>
        require(requirement = false, s"not supported core config: $l3")
        new MinimalCoreConfig
    }
    val l3Cfg = l3 match {
      case "full" => new FullL3Config
      case "medium" => new MediumL3Config
      case "small" => new SmallL3Config
      case "extreme" => new ExtremeL3Config
      case _ =>
        require(requirement = false, s"not supported l3 config: $l3")
        new ExtremeL3Config
    }
    val nocCfg = noc match {
      case "full" => new FullNocConfig(socket)
      case "medium" => new ReducedNocConfig(socket)
      case "small" => new MinimalNocConfig(socket)
      case "extreme" => new ExtremeNocConfig(socket)
      case _ =>
        require(requirement = false, s"not supported noc config: $noc")
        new MinimalNocConfig(socket)
    }
    new Config(
      coreCfg ++ l3Cfg ++ nocCfg ++ new BaseConfig
    )
  }
  private var core = ""
  private var l3 = ""
  private var noc = ""
  private var socket = ""
  private var opts = Array[String]()

  @tailrec
  private def doParse(args: List[String]): Unit = {
    args match {
      case "--core" :: cfgStr :: tail =>
        core = cfgStr
        doParse(tail)

      case "--l3" :: cfgStr :: tail =>
        l3 = cfgStr
        doParse(tail)

      case "--noc" :: cfgStr :: tail =>
        noc = cfgStr
        doParse(tail)

      case "--socket" :: cfgStr :: tail =>
        socket = cfgStr
        doParse(tail)

      case option :: tail =>
        opts :+= option
        doParse(tail)

      case Nil =>
    }
  }

  def parse(args: List[String]):(Parameters, Array[String]) = {
    doParse(args)
    (generate(core, l3, noc, socket), opts)
  }
}