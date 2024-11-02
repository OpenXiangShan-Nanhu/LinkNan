package linknan.generator

import org.chipsalliance.cde.config.Parameters
import xs.utils.perf.DebugOptionsKey
import zhujiang.ZJParametersKey

object ArgParser {
  def configParse(sim:Boolean)(args: Array[String]) :(Parameters, Array[String]) = {
    val configParam = args.filter(_ == "--config")
    if(configParam.isEmpty) {
      val defaultCfg = if(sim) {
        println("Config is not assigned, use Minimal Configuration!")
        new MinimalConfig
      } else {
        println("Config is not assigned, use Full Configuration!")
        new FullConfig
      }
      (defaultCfg, args)
    } else {
      val pos = args.indexOf(configParam.head)
      val cfgStr = args(pos + 1)
      val res = cfgStr match {
        case "reduced" => new ReducedConfig
        case "minimal" => new MinimalConfig
        case "spec" => new SpecConfig
        case "fpga" => new FpgaConfig
        case _ => new FullConfig
      }
      val newArgs = args.zipWithIndex.filterNot(e => e._2 == pos || e._2 == (pos + 1)).map(_._1)
      (res, newArgs)
    }
  }

  def apply(args: Array[String]): (Parameters, Array[String]) = {
    val (configuration, stripCfgArgs) = configParse(sim = false)(args)

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
            case RemoveCoreKey => true
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

    val cfg = parse(configuration, stripCfgArgs.toList)
    (cfg, firrtlOpts)
  }
}
