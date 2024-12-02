package linknan.generator

import org.chipsalliance.cde.config.Parameters
import xs.utils.perf.DebugOptionsKey
import zhujiang.ZJParametersKey

import scala.annotation.tailrec

object ArgParser {
  private var core = "nanhu"
  private var cfg = "full"
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
      case "reduced" => new ReducedConfig(core)
      case "minimal" => new MinimalConfig(core)
      case "spec" => new SpecConfig(core)
      case "fpga" => new FpgaConfig(core)
      case "btest" => new BtestConfig(core)
      case _ => new FullConfig(core)
    }
    println(s"Using $cfg config with $core cores")

    var firrtlOpts = Array[String]()
    def parse(config: Parameters, args: List[String]): Parameters = {
      args match {
        case Nil => config

        case "--fpga-platform" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(FPGAPlatform = true)
          }), tail)

        case "--cpu-sync" :: tail =>
          parse(config.alter((site, here, up) => {
            case ZJParametersKey => up(ZJParametersKey).copy(cpuAsync = false)
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
            case TestIoOptionsKey => up(TestIoOptionsKey).copy(removeCore = true)
          }), tail)

        case "--no-csu" :: tail =>
          parse(config.alter((site, here, up) => {
            case TestIoOptionsKey => up(TestIoOptionsKey).copy(removeCsu = true)
          }), tail)

        case "--prefix" :: confString :: tail =>
          parse(config.alter((site, here, up) => {
            case PrefixKey => confString
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
