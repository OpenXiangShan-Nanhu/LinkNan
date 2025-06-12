package lntest.top

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import linknan.generator.Generator
import linknan.soc.{LNTop, LinkNanParamsKey}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.DisableMonitors
import xs.utils.FileRegisters
import xs.utils.perf.DebugOptionsKey
import xs.utils.stage.XsStage
import zhujiang.{NocIOHelper, ZJParametersKey}
import zhujiang.axi.AxiUtils

class FpgaTop(implicit val p: Parameters) extends Module with NocIOHelper {
  override val desiredName = "XlnFpgaTop"
  override def resetType = Module.ResetType.Asynchronous
  private val soc = Module(new LNTop)
  val io = IO(new Bundle {
    val core_clk = Input(Vec(soc.io.cluster_clocks.size, Clock()))
    val noc_clk = Input(Clock())
    val rtc_clk = Input(Bool())
    val reset_vector = Input(UInt(soc.io.default_reset_vector.getWidth.W))
    val ext_intr = Input(UInt(soc.io.ext_intr.getWidth.W))
  })

  soc.io.cluster_clocks := io.core_clk
  soc.io.noc_clock := io.noc_clk
  soc.io.rtc_clock := io.rtc_clk
  soc.io.ext_intr := io.ext_intr
  soc.io.default_reset_vector := io.reset_vector
  soc.io.reset := reset
  soc.io.dft := DontCare
  soc.io.ramctl := 0.U
  soc.io.ci := 0.U
  soc.io.dft.lgc_rst_n := true.B
  soc.io.jtag.foreach(_ := DontCare)
  soc.io.jtag.foreach(_.reset := true.B)

  val ddrDrv = soc.ddrIO.map(AxiUtils.getIntnl)
  val cfgDrv = soc.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = soc.dmaIO.map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = soc.hwaIO.map(AxiUtils.getIntnl)
}

object FpgaGenerator extends App {
  val (config, firrtlOpts) = SimArgParser(args)
  xs.utils.GlobalData.prefix = config(LinkNanParamsKey).prefix
  difftest.GlobalData.prefix = config(LinkNanParamsKey).prefix
  (new XsStage).execute(firrtlOpts, Generator.firtoolOpts ++ Seq(
    ChiselGeneratorAnnotation(() => {
      DisableMonitors(p => new FpgaTop()(p))(config.alter((site, here, up) => {
        case ZJParametersKey => config(ZJParametersKey).copy(tfbParams = None)
        case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDifftest = false)
      }))
    })
  ))
  FileRegisters.write(filePrefix = config(LinkNanParamsKey).prefix + "LNTop.")
}