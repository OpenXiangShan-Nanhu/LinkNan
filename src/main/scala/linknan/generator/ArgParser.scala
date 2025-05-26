package linknan.generator

import xs.utils.cache.common.L2ParamKey
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import xs.utils.debug.HardwareAssertionKey
import xs.utils.perf.{DebugOptionsKey, LogUtilsOptionsKey, PerfCounterOptionsKey}
import zhujiang.ZJParametersKey

import scala.annotation.tailrec

object ArgParser {
  def apply(args: Array[String]): (Parameters, Array[String]) = {
    val (configuration, opts) = ConfigGenerater.parse(args.toList)

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

        case "--legacy" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(
              internalDeviceMax = 0x4000_0000L,
              mswiBase = 0x3800_0000L,
              plicBase = 0x3C00_0000L
            )
          }), tail)

        case "--basic-difftest" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(AlwaysBasicDiff = true)
          }), tail)

        case "--no-core" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(removeCore = true)
          }), tail)

        case "--keep-l1c" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(keepL1c = true)
          }), tail)

        case "--enable-hardware-assertion" :: tail =>
          parse(config.alter((site, here, up) => {
            case HardwareAssertionKey => up(HardwareAssertionKey).copy(enable = true)
          }), tail)

        case "--enable-mbist" :: tail =>
          parse(config.alter((site, here, up) => {
            case XSCoreParamsKey => up(XSCoreParamsKey).copy(hasMbist = true)
            case L2ParamKey => up(L2ParamKey).copy(hasMbist = true)
            case ZJParametersKey => up(ZJParametersKey).copy(hasMbist = true)
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
