package lntest.top

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import linknan.generator.Generator
import linknan.soc.{LNTop, LinkNanParamsKey}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.DisableMonitors
import xs.utils.{FileRegisters, ResetGen}
import xs.utils.perf.DebugOptionsKey
import xs.utils.stage.XsStage
import zhujiang.{NocIOHelper, ZJParametersKey}
import zhujiang.axi.AxiUtils

class FpgaTop(implicit val p: Parameters) extends RawModule with NocIOHelper {
  override val desiredName = "XlnFpgaTop"
  private val soc = Module(new LNTop)
  val io = IO(new Bundle {
    val resetn = Input(Bool())
    val core_clk = Input(Vec(soc.io.cluster_clocks.size, Clock()))
    val noc_clk = Input(Clock())
    val rtc_clk = Input(Bool())
    val reset_vector = Input(UInt(soc.io.default_reset_vector.getWidth.W))
    val ext_intr = Input(UInt(soc.io.ext_intr.getWidth.W))
  })
  private val _reset = (!io.resetn).asAsyncReset
  private val resetSync = withClockAndReset(io.noc_clk, _reset) { ResetGen(2, None) }

  soc.io.cluster_clocks := io.core_clk
  soc.io.noc_clock := io.noc_clk
  soc.io.rtc_clock := io.rtc_clk
  soc.io.ext_intr := io.ext_intr
  soc.io.default_reset_vector := io.reset_vector
  soc.io.reset := resetSync
  soc.io.dft := DontCare
  soc.io.ramctl := DontCare
  soc.io.ci := 0.U
  soc.io.dft.lgc_rst_n := true.B
  soc.io.jtag.foreach(_ := DontCare)
  soc.io.jtag.foreach(_.reset := true.B)

  val ddrDrv = soc.ddrIO.map(AxiUtils.getIntnl)
  val cfgDrv = soc.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = soc.dmaIO.map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = soc.hwaIO.map(AxiUtils.getIntnl)
  runIOAutomation()
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