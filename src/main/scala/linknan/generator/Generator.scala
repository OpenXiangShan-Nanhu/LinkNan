package linknan.generator

import chisel3.stage.ChiselGeneratorAnnotation
import circt.stage.FirtoolOption
import linknan.soc.{LNTop, LinkNanParamsKey}
import xijiang.tfb.TrafficBoardFileManager
import xs.utils.FileRegisters
import xs.utils.stage.XsStage
import zhujiang.ZJParametersKey

object Generator {
  def firtoolOpts(random:Boolean) = {
    val opts = Seq(
      FirtoolOption("-O=release"),
      FirtoolOption("--disable-annotation-unknown"),
      FirtoolOption("--strip-debug-info"),
      FirtoolOption("--lower-memories"),
      FirtoolOption("--add-vivado-ram-address-conflict-synthesis-bug-workaround"),
      FirtoolOption("--lowering-options=noAlwaysComb," +
        " disallowPortDeclSharing, disallowLocalVariables," +
        " emittedLineLength=120, explicitBitcast," +
        " locationInfoStyle=plain, disallowMuxInlining")
    )
    if(random) opts
    else opts :+ FirtoolOption("--disable-all-randomization")
  }
}

object SocGenerator extends App {
  val (config, firrtlOpts) = ArgParser(args)
  xs.utils.GlobalData.prefix = config(LinkNanParamsKey).prefix
  difftest.GlobalData.prefix = config(LinkNanParamsKey).prefix
  (new XsStage).execute(firrtlOpts, Generator.firtoolOpts(config(LinkNanParamsKey).random) ++ Seq(
    ChiselGeneratorAnnotation(() => new LNTop()(config))
  ))

  if(config(ZJParametersKey).tfbParams.isDefined) TrafficBoardFileManager.release("cosim", "cosim")(config)
  FileRegisters.write(filePrefix = config(LinkNanParamsKey).prefix + "LNTop.")
}
