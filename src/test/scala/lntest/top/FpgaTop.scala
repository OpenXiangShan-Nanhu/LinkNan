package lntest.top

import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util.HasBlackBoxInline
import chisel3.{BlackBox, _}
import freechips.rocketchip.jtag.JTAGIO
import linknan.generator.{AddrConfig, Generator}
import linknan.soc.{LNTop, LinkNanParamsKey}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.DisableMonitors
import xs.utils.perf.DebugOptionsKey
import xs.utils.stage.XsStage
import xs.utils.{FileRegisters, ResetGen}
import zhujiang.axi.{AxiBufferChain, AxiBundle, AxiUtils}
import zhujiang.{NocIOHelper, ZJRawModule}

class VerilogAddrRemapper(width:Int) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val a = Input(UInt(width.W))
    val b = Input(UInt(width.W))
    val c = Input(UInt(width.W))
    val z = Output(UInt(width.W))
  })
  setInline(s"VerilogAddrRemapper.sv",
    s"""module VerilogAddrRemapper (
       |  input  wire [${width - 1}:0] a,
       |  input  wire [${width - 1}:0] b,
       |  input  wire [${width - 1}:0] c,
       |  output wire [${width - 1}:0] z
       |);
       |  assign z = a - b + c;
       |endmodule""".stripMargin)
}

class FpgaClkDiv10 extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val CK = Input(Clock())
    val Q = Output(Clock())
  })
  setInline(s"FpgaClkDiv10.sv",
    s"""module FpgaClkDiv10 (
       |  input  wire CK,
       |  output wire Q
       |);
       |  reg  [3:0]   div_reg;
       |  reg          rtc_reg;
       |`ifndef SYNTHESIS
       |  initial div_reg = 0;
       |  initial rtc_reg = 0;
       |`endif
       |  always @(posedge CK) begin
       |    if (div_reg > 4'h8) begin
       |      div_reg <= 4'h0;
       |    end else begin
       |      div_reg <= div_reg + 4'h1;
       |    end
       |    rtc_reg <= div_reg > 4'h4;
       |  end
       |  assign Q = rtc_reg;
       |endmodule""".stripMargin)
}

object VerilogAddrRemapper {
  def apply(a:UInt, b:UInt, c:UInt):UInt = {
    val rmp = Module(new VerilogAddrRemapper(a.getWidth.max(b.getWidth)))
    rmp.io.a := a
    rmp.io.b := b
    rmp.io.c := c
    rmp.io.z
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
    val reset_vector = Input(UInt(raw.W))
    val ddr_offset = Input(UInt(raw.W))
    val ext_intr = Input(UInt(soc.io.ext_intr.getWidth.W))
    val systemjtag = Option.when(soc.io.jtag.isDefined)(new Bundle {
      val jtag = Flipped(new JTAGIO(hasTRSTn = false))
      val reset = Input(AsyncReset())
    })
  })
  private val _reset = (!io.aresetn).asAsyncReset
  private val resetSync = withClockAndReset(io.aclk, _reset) { ResetGen(2, None) }
  val implicitClock = io.aclk
  val implicitReset = resetSync

  private val rtc_div = Module(new FpgaClkDiv10)
  private val ddrPorts = soc.ddrIO.filterNot(_.params.attr.contains("hs"))
  private val pcieMst = soc.ddrIO.filter(_.params.attr.contains("hs"))
  soc.ddrIO.filter(_.params.attr.contains("hs")).foreach(_ := DontCare)
  private val ddrXbar = Module(new SimNto1Bridge(ddrPorts.map(_.params)))
  private val portP = ddrXbar.io.downstream.head.params.copy(attr = "mem_0")
  private val ddrBuf = Module(new AxiBufferChain(portP, 32))
  for((a, b) <- ddrXbar.io.upstream.zip(ddrPorts)) {
    a <> b
    a.ar.bits.addr := VerilogAddrRemapper(b.araddr, AddrConfig.pmemRange.lower.U(raw.W), io.ddr_offset)
    a.aw.bits.addr := VerilogAddrRemapper(b.awaddr, AddrConfig.pmemRange.lower.U(raw.W), io.ddr_offset)
  }
  ddrBuf.io.in <> ddrXbar.io.downstream.head
  rtc_div.io.CK := io.rtc_clk

  soc.io.cluster_clocks := io.core_clk
  soc.io.noc_clock := io.aclk
  soc.io.dev_clock := io.aclk
  soc.io.rtc_clock := rtc_div.io.Q.asBool
  soc.io.ext_intr := io.ext_intr
  soc.io.default_reset_vector := io.reset_vector
  soc.io.default_cpu_enable.foreach(_ := true.B)
  soc.io.reset := resetSync
  soc.io.dft := DontCare
  soc.io.ramctl := DontCare
  soc.io.ci := 0.U
  soc.io.dft.lgc_rst_n := true.B
  soc.io.jtag.foreach(j => {
    j.mfr_id := 0x11.U
    j.part_number := 0x16.U
    j.version := 4.U
    j.reset := io.systemjtag.get.reset
  })
  io.systemjtag.foreach(_.jtag <> soc.io.jtag.get.jtag)
  soc.dmaIO.foreach(_ := DontCare)

  val ddrDrv = Seq(ddrBuf.io.out) ++ pcieMst.map(AxiUtils.getIntnl)
  val cfgDrv = soc.cfgIO.map(AxiUtils.getIntnl)
  val dmaDrv = soc.dmaIO.filter(_.params.dataBits > 64).map(AxiUtils.getIntnl)
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
        case DebugOptionsKey => up(DebugOptionsKey).copy(EnableDifftest = false)
      }))
    })
  ))
  FileRegisters.write(filePrefix = config(LinkNanParamsKey).prefix + "LNTop.")
}