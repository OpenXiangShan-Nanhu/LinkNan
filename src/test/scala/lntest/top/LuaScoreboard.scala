package lntest.top

import chisel3._
import chisel3.util._

class LuaScoreboard(l2Str:String, nrHnf:Int) extends BlackBox(
  Map(
    "L2_CFG_STR" -> l2Str,
    "NR_HNF" -> nrHnf
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
       |  parameter string L2_CFG_STR,
       |  parameter NR_HNF
       |)(
       |  input  wire clock,
       |  input  wire reset,
       |  input  wire sim_final
       |);
       |`ifndef SYNTHESIS
       |  import "DPI-C" function void verilua_final();
       |  import "DPI-C" function void verilua_main_step_safe();
       |`ifdef VERILATOR
       |  initial begin
       |    $$c("char value[500];");
       |
       |    $$c("setenv(\\"L2_CFG_STR\\",", L2_CFG_STR, ".c_str(), 1);");
       |
       |    $$c("sprintf(value, \\"%d\\",", NR_HNF, ");");
       |    $$c("setenv(\\"NR_HNF\\", value, 1);");
       |  end
       |`else // VERILATOR
       |  import "DPI-C" function void setenv(string name, string value, integer replace);
       |
       |  initial begin 
       |    setenv("L2_CFG_STR", L2_CFG_STR, 1);
       |    setenv("NR_HNF", $$sformatf("%d", NR_HNF), 1);
       |  end
       |`endif // VERILATOR
       |
       |always @ (negedge clock) begin
       |  if(~reset) verilua_main_step_safe();
       |  if(sim_final) verilua_final();
       |end
       |
       |`ifndef VERILATOR
       |final verilua_final();
       |`endif // VERILATOR
       |
       |`endif // SYNTHESIS
       |endmodule
       |
       |""".stripMargin)
}
