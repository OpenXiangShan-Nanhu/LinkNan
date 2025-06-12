package lntest.top

import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util.HasBlackBoxInline
import linknan.generator.{AddrConfig, Generator}
import linknan.soc.{LNTop, LinkNanParamsKey}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.DisableMonitors
import xs.utils.{FileRegisters, ResetGen}
import xs.utils.perf.DebugOptionsKey
import xs.utils.stage.XsStage
import zhujiang.{NocIOHelper, ZJParametersKey, ZJRawModule}
import zhujiang.axi.AxiUtils

class VerilogMinus(width:Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val a = Input(UInt(width.W))
    val b = Input(UInt(width.W))
    val z = Output(UInt(width.W))
  })
  setInline(s"VerilogMinus.sv",
    s"""module VerilogMinus (
       |  input  wire [${width - 1}:0] a,
       |  input  wire [${width - 1}:0] b,
       |  output wire [${width - 1}:0] z
       |);
       |  assign z = a - b;
       |endmodule""".stripMargin)
}

object VerilogMinus {
  def apply(a:UInt, b:UInt):UInt = {
    val minus = Module(new VerilogMinus(a.getWidth.max(b.getWidth)))
    minus.io.a := a
    minus.io.b := b
    minus.io.z
  }
}

class FpgaTop(implicit p: Parameters) extends ZJRawModule with NocIOHelper {
  override val desiredName = "XlnFpgaTop"
  private val soc = Module(new LNTop)
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val core_clk = Input(Vec(soc.io.cluster_clocks.size, Clock()))
    val noc_clk = Input(Clock())
    val rtc_clk = Input(Clock())
    val reset_vector = Input(UInt(soc.io.default_reset_vector.getWidth.W))
    val ext_intr = Input(UInt(soc.io.ext_intr.getWidth.W))
  })
  private val _reset = (!io.aresetn).asAsyncReset
  private val resetSync = withClockAndReset(io.noc_clk, _reset) { ResetGen(2, None) }
  private val _rtc_reg = withClockAndReset(io.rtc_clk, _reset) { RegInit(false.B) }
  _rtc_reg := ~_rtc_reg

  soc.io.cluster_clocks := io.core_clk
  soc.io.noc_clock := io.noc_clk
  soc.io.rtc_clock := _rtc_reg
  soc.io.ext_intr := io.ext_intr
  soc.io.default_reset_vector := io.reset_vector
  soc.io.reset := resetSync
  soc.io.dft := DontCare
  soc.io.ramctl := DontCare
  soc.io.ci := 0.U
  soc.io.dft.lgc_rst_n := true.B
  soc.io.jtag.foreach(_ := DontCare)
  soc.io.jtag.foreach(_.reset := true.B)
  soc.dmaIO.filter(_.params.dataBits < 256).foreach(_ := DontCare)

  val ddrDrv = soc.ddrIO.map(AxiUtils.getIntnl)
  val cfgDrv = soc.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = soc.dmaIO.filter(_.params.dataBits == 256).map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = soc.hwaIO.map(AxiUtils.getIntnl)
  runIOAutomation()
  ddrIO.zip(ddrDrv).foreach({case(a, b) =>
    a.araddr := VerilogMinus(b.ar.bits.addr, AddrConfig.pmemRange.lower.U(raw.W))
    a.awaddr := VerilogMinus(b.aw.bits.addr, AddrConfig.pmemRange.lower.U(raw.W))
  })
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