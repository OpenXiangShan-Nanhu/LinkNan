package lntest.top

import freechips.rocketchip.diplomacy.AddressSet
import linknan.generator._
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import xs.utils.cache.common.L2ParamKey
import xs.utils.debug.HardwareAssertionKey
import xs.utils.perf.{DebugOptionsKey, LogUtilsOptionsKey, PerfCounterOptionsKey}
import zhujiang.ZJParametersKey

import scala.annotation.tailrec

object SimArgParser {
  def apply(args: Array[String]): (Parameters, Array[String]) = {
    val (configuration, opts) = ConfigGenerater.parse(args.toList)

    var firrtlOpts = Array[String]()

    @tailrec
    def parse(config: Parameters, args: List[String]): Parameters = {
      args match {
        case Nil => config

        case "--dramsim3" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(UseDRAMSim = true)
          }), tail)

        case "--no-perf" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnablePerfDebug = false)
            case PerfCounterOptionsKey =>up(PerfCounterOptionsKey).copy(enablePerfPrint = false)
          }), tail)
        case "--no-tfb" :: tail =>
          parse(config.alter((site, here, up) => {
            case ZJParametersKey => up(ZJParametersKey).copy(tfbParams = None)
          }), tail)
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
              internalDeviceAdressSets = up(LinkNanParamsKey).internalDeviceAdressSets :+ AddressSet(0x3800_0000L, 0x07FF_FFFF),
              mswiBase = 0x3800_0000L,
              plicBase = 0x3C00_0000L
            )
          }), tail)

        case "--basic-difftest" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(AlwaysBasicDiff = true)
          }), tail)

        case "--no-mem" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(removeMem = true)
          }), tail)

        case "--no-core" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(removeCore = true)
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDebug = true)
          }), tail)

        case "--keep-l1c" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(keepL1c = true)
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDebug = true)
          }), tail)

        case "--no-extra-nc-mem" :: tail =>
          parse(config.alter((site, here, up) => {
            case LinkNanParamsKey => up(LinkNanParamsKey).copy(extraNcMem = false)
          }), tail)

        case "--lua-scoreboard" :: tail =>
          parse(config.alter((site, here, up) => {
            case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDebug = true, EnableLuaScoreBoard = true, FPGAPlatform = true)
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

        case "--difftest-config" :: confString :: tail =>
          difftest.gateway.Gateway.setConfig(confString)
          parse(config, tail)

        case option :: tail =>
          firrtlOpts :+= option
          parse(config, tail)
      }
    }

    val finalCfg = parse(configuration, opts.toList)
    (finalCfg, firrtlOpts)
  }
}
