package lntest.top

import chisel3._
import chisel3.util._

class LuaScoreboard(nrL2:Int, nrL2Bank:Int, nrPcu:Int, nrDcu:Int) extends BlackBox(
  Map(
    "NR_L2" -> nrL2,
    "NR_L2_BANK" -> nrL2Bank,
    "NR_PCU" -> nrPcu,
    "NR_DCU" -> nrDcu
  )
) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val sim_final = Input(Bool())
  })
  setInline(s"LuaScoreboard.sv",
    s"""
       |module LuaScoreboard #(
       |  parameter NR_L2,
       |  parameter NR_L2_BANK,
       |  parameter NR_PCU,
       |  parameter NR_DCU
       |)(
       |  input  wire clock,
       |  input  wire reset,
       |  input  wire sim_final
       |);
       |`ifndef SYNTHESIS
       |  import "DPI-C" function void verilua_init();
       |  import "DPI-C" function void verilua_final();
       |  import "DPI-C" function void verilua_main_step_safe();
       |`ifdef VERILATOR
       |  initial verilua_init();
       |`else
       |  initial #1 verilua_init();
       |`endif
       |
       |always @ (negedge clock) begin
       |  if(~reset) verilua_main_step_safe();
       |  if(sim_final) verilua_final();
       |end
       |
       |`endif
       |endmodule
       |
       |""".stripMargin)
}
