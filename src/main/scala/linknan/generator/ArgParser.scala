package linknan.generator

import xs.utils.cacheParam.common.L2ParamKey
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import xs.utils.debug.HardwareAssertionKey
import xs.utils.perf.{DebugOptionsKey, LogUtilsOptionsKey, PerfCounterOptionsKey}
import zhujiang.ZJParametersKey

import scala.annotation.tailrec

object ArgParser {
  private var core = "nanhu"
  private var cfg = "full"
  private var socket = "async"
  private var opts = Array[String]()
  @tailrec
  def configParse(args: List[String]): Unit = {
    args match {
      case "--config" :: cfgStr :: tail => {
        cfg = cfgStr
        configParse(tail)
      }
      case "--core" :: cfgStr :: tail => {
        core = cfgStr
        configParse(tail)
      }
      case "--socket" :: cfgStr :: tail => {
        socket = cfgStr
        configParse(tail)
      }
      case option :: tail => {
        opts :+= option
        configParse(tail)
      }
      case Nil =>
    }
  }

  def apply(args: Array[String]): (Parameters, Array[String]) = {
    configParse(args.toList)
    val configuration = cfg match {
      case "reduced" => new ReducedConfig(core, socket)
      case "extreme" => new ExtremeConfig(core, socket)
      case "minimal" => new MinimalConfig(core, socket)
      case "spec" => new SpecConfig(core, socket)
      case "fpga" => new FpgaConfig(core, socket)
      case "btest" => new BtestConfig(core, socket)
      case _ => new FullConfig(core, socket)
    }
    println(s"Using $cfg config with $core cores and use $socket socket")

    var firrtlOpts = Array[String]()
    def parse(config: Parameters, args: List[String]): Parameters = {
      args match {
        case Nil => config

        case "--fpga-platform" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnablePerfDebug = false, FPGAPlatform = true)
            case PerfCounterOptionsKey =>up(PerfCounterOptionsKey).copy(enablePerfPrint = false)
            case LogUtilsOptionsKey => up(LogUtilsOptionsKey).copy(fpgaPlatform = true)
          }), tail)

        case "--enable-difftest" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDifftest = true)
          }), tail)

        case "--basic-difftest" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(AlwaysBasicDiff = true)
          }), tail)

        case "--no-cores" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(removeCore = true)
          }), tail)

        case "--enable-hardware-assertion" :: tail =>
          parse(config.alter((site, here, up) => {
            case HardwareAssertionKey => up(HardwareAssertionKey).copy(enable = true)
          }), tail)

        case "--enable-mbist" :: tail =>
          parse(config.alter((site, here, up) => {
            case XSCoreParamsKey => up(XSCoreParamsKey).copy(hasMbist = true)
            case L2ParamKey => up(L2ParamKey).copy(hasMbist = true)
          }), tail)

        case "--prefix" :: confString :: tail =>
          parse(config.alter((site, here, up) => {
            case ZJParametersKey => up(ZJParametersKey).copy(modulePrefix = confString)
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(prefix = confString)
          }), tail)

        case option :: tail =>
          firrtlOpts :+= option
          parse(config, tail)
      }
    }

    val finalCfg = parse(configuration, opts.toList)
    (finalCfg, firrtlOpts)
  }
}
