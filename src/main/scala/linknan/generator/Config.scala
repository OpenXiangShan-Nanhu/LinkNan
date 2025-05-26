package linknan.generator

import chisel3.util.log2Ceil
import coupledL2.prefetch.PrefetchReceiverParams
import freechips.rocketchip.diplomacy.{AddressRange, AddressSet}
import linknan.soc.{LinkNanParams, LinkNanParamsKey}
import org.chipsalliance.cde.config.{Config, _}
import xiangshan.backend.fu.{MemoryRange, PMAConfigEntry}
import xiangshan.{PMParameKey, PMParameters, XSCoreParameters, XSCoreParamsKey}
import xiangshan.cache.DCacheParameters
import xiangshan.frontend.icache.ICacheParameters
import xijiang.{NodeParam, NodeType}
import xs.utils.cache.L2Param
import xs.utils.cache.common.L2ParamKey
import xs.utils.cache.prefetch.BOPParameters
import xs.utils.debug.{HardwareAssertionKey, HwaParams}
import xs.utils.perf.{DebugOptions, DebugOptionsKey, LogUtilsOptions, LogUtilsOptionsKey, PerfCounterOptions, PerfCounterOptionsKey, XSPerfLevel}
import zhujiang.axi.AxiParams
import zhujiang.device.AxiDeviceParams
import zhujiang.{ZJParameters, ZJParametersKey}

import scala.annotation.tailrec

class BaseConfig extends Config((site, here, up) => {
  case HardwareAssertionKey => HwaParams()
  case DebugOptionsKey => DebugOptions()
  case L2ParamKey => L2Param(hasMbist = false)
  case XSCoreParamsKey => XSCoreParameters(hasMbist = false)
  case LogUtilsOptionsKey => LogUtilsOptions(enableDebug = false, enablePerf = true, fpgaPlatform = false)
  case PerfCounterOptionsKey => PerfCounterOptions(enablePerfPrint = true, enablePerfDB = false, XSPerfLevel.VERBOSE, 0)
  case LinkNanParamsKey => LinkNanParams()
  case PMParameKey => PMParameters(
    PmemRanges = Seq(AddrConfig.pmemRange),
    PMAConfigs = Seq(
      PMAConfigEntry(AddrConfig.pmemRange.upper, c = true, atomic = true, a = 1, x = true, w = true, r = true),
      PMAConfigEntry(AddrConfig.pmemRange.lower, a = 1, w = true, r = true, x = true),
      PMAConfigEntry(0)
    )
  )
})

object AddrConfig {
  // interleaving granularity: 1KiB
  // DDR: 0x0_8000_0000 ~ 0xF_FFFF_FFFF
  val interleaveOffset = 6
  val pmemRange = MemoryRange(0x00_8000_0000L, 0x10_0000_0000L)
  private val interleaveBits = 2
  private val interleaveMask = ((0x1L << interleaveBits) - 1) << interleaveOffset
  private val memFullMask = (1L << log2Ceil(pmemRange.upper)) - 1
  private val everything = AddressSet(0x0L, memFullMask)

  private val memFullAddrSet = everything.subtract(AddressSet(0x0L, (1L << log2Ceil(pmemRange.lower)) - 1))

  def memBank(bank: Long):Seq[(Long, Long)] = {
    require(bank < (0x1L << interleaveBits))
    memFullAddrSet.map(as => {
      val base = as.base | (bank << interleaveOffset)
      val mask = (as.mask.toLong ^ memFullMask) | interleaveMask
      (base.toLong, mask)
    })
  }

  val mem0 = memBank(0)
  val mem1 = memBank(1)
  val mem2 = memBank(2)
  val mem3 = memBank(3)

  val mem_uc = Seq(
    (0x000_4000_0000L, 0xFFF_C000_0000L),
    (0x000_5000_0000L, 0xFFF_C000_0000L),
    (0x000_6000_0000L, 0xFFF_C000_0000L),
    (0x000_7000_0000L, 0xFFF_C000_0000L),
  )
}

class FullNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    hnxBankOff = AddrConfig.interleaveOffset,
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.P),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_0"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_1"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_2"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_3"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_4"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "north", "mem_n_5"))),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),
      NodeParam(nodeType = NodeType.P),

      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "east", "e_0")), addrSets = AddrConfig.mem2),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "east", "e_1")), addrSets = AddrConfig.mem3),

      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 1),
      NodeParam(nodeType = NodeType.HI, axiDevParams = Some(AxiDeviceParams(0, 32, "south", "main")), defaultHni = true),
      NodeParam(nodeType = NodeType.RH, axiDevParams = Some(AxiDeviceParams(0, 32, "south", "main", Some(AxiParams(idBits = 14))))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(0, 32, "south", "mem_s_hs"))),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "south", "s_uc")), addrSets = AddrConfig.mem_uc),
      NodeParam(nodeType = NodeType.M,  axiDevParams = Some(AxiDeviceParams(3, 32, "south"))),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, socket = socket),

      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "west", "w_0")), addrSets = AddrConfig.mem0),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "west", "w_1")), addrSets = AddrConfig.mem1),
    )
  )
})

class ReducedNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    hnxBankOff = AddrConfig.interleaveOffset,
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, socket = socket),

      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main", Some(AxiParams(idBits = 14))))),
      NodeParam(nodeType = NodeType.HI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main")), defaultHni = true),

      NodeParam(nodeType = NodeType.HF, bankId = 2, hfpId = 0),
      NodeParam(nodeType = NodeType.HF, bankId = 3, hfpId = 0),

      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "default", "0")), addrSets = AddrConfig.mem0),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "default", "1")), addrSets = AddrConfig.mem1),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "default", "2")), addrSets = AddrConfig.mem2),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "default", "3")), addrSets = AddrConfig.mem3),

      NodeParam(nodeType = NodeType.M),
      NodeParam(nodeType = NodeType.P),
    )
  )
})

class MinimalNocConfig(socket: String, ddrBuf:Int) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    hnxBankOff = AddrConfig.interleaveOffset,
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 0),

      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main", Some(AxiParams(idBits = 14))))),
      NodeParam(nodeType = NodeType.HI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main")), defaultHni = true),

      NodeParam(nodeType = NodeType.HF, bankId = 1, hfpId = 1),
      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(ddrBuf, 64, "memsys"))),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),

      NodeParam(nodeType = NodeType.M),
      NodeParam(nodeType = NodeType.P)
    )
  )
})

class ExtremeNocConfig(socket: String) extends Config((site, here, up) => {
  case ZJParametersKey => ZJParameters(
    hnxBankOff = AddrConfig.interleaveOffset,
    nodeParams = Seq(
      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 0),
      NodeParam(nodeType = NodeType.CC, socket = socket),

      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "default", "mem_0"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(1, 64, "default", "mem_1"))),
      NodeParam(nodeType = NodeType.RI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main", Some(AxiParams(idBits = 15))))),
      NodeParam(nodeType = NodeType.HI, axiDevParams = Some(AxiDeviceParams(0, 32, "default", "main")), defaultHni = true),

      NodeParam(nodeType = NodeType.CC, socket = socket),
      NodeParam(nodeType = NodeType.HF, bankId = 0, hfpId = 1),
      NodeParam(nodeType = NodeType.CC, socket = socket),

      NodeParam(nodeType = NodeType.S,  axiDevParams = Some(AxiDeviceParams(1, 32, "default"))),
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
  new LLCConfig(2 * 1024 * 1024, 8, 8, 64, 2)
)

// use extreme l3 config require the number of HNF no more than 4 in NoC
class ExtremeL3Config extends Config(
  new LLCConfig(64 * 16, 4, 4, 16, 1)
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
      case "small" => new MinimalNocConfig(socket, 0)
      case "fpga" => new MinimalNocConfig(socket, 32)
      case "extreme" => new ExtremeNocConfig(socket)
      case _ =>
        require(requirement = false, s"not supported noc config: $noc")
        new MinimalNocConfig(socket, 0)
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