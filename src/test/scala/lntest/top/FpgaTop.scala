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
import zhujiang.axi.{AxiBufferChain, AxiBundle, AxiUtils, ExtAxiBundle}

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

class FpgaTop(implicit p: Parameters) extends ZJRawModule with NocIOHelper with ImplicitClock with ImplicitReset {
  override val desiredName = "XlnFpgaTop"
  private val soc = Module(new LNTop)
  val io = IO(new Bundle {
    val aresetn = Input(Bool())
    val aclk = Input(Clock())
    val core_clk = Input(Vec(soc.io.cluster_clocks.size, Clock()))
    val rtc_clk = Input(Clock())
    val reset_vector = Input(UInt(soc.io.default_reset_vector.getWidth.W))
    val ext_intr = Input(UInt(soc.io.ext_intr.getWidth.W))
  })
  private val _reset = (!io.aresetn).asAsyncReset
  private val resetSync = withClockAndReset(io.aclk, _reset) { ResetGen(2, None) }
  private val _rtc_reg = withClockAndReset(io.rtc_clk, _reset) { RegInit(false.B) }
  _rtc_reg := ~_rtc_reg
  val implicitClock = io.aclk
  val implicitReset = resetSync

  soc.io.cluster_clocks := io.core_clk
  soc.io.noc_clock := io.aclk
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

  private def bufferMstAxi(in:ExtAxiBundle, depth:Int):AxiBundle = {
    val bufChains = Module(new AxiBufferChain(in.params, depth))
    bufChains.io.in <> in
    bufChains.io.out
  }
  private def bufferSlvAxi(out:ExtAxiBundle, depth:Int):AxiBundle = {
    val bufChains = Module(new AxiBufferChain(out.params, depth))
    out <> bufChains.io.out
    bufChains.io.in
  }

  private val ddrBufOuts = soc.ddrIO.map(bufferMstAxi(_, 32))
  private val cfgBufOuts = soc.cfgIO.map(bufferMstAxi(_, 2))
  private val dmaBufIns = soc.dmaIO.map(bufferSlvAxi(_, 2))

  val ddrDrv = ddrBufOuts
  val cfgDrv = cfgBufOuts
  val dmaDrv = dmaBufIns
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