package lntest.top

import chisel3._
import chisel3.util._

class LuaScoreboard(l2Str:String, nrPcu:Int, nrDcu:Int, dcuStr:String) extends BlackBox(
  Map(
    "L2_CFG_STR" -> l2Str,
    "NR_PCU" -> nrPcu,
    "NR_DCU" -> nrDcu,
    "DCU_NODE_STR" -> dcuStr
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
       |  parameter NR_PCU,
       |  parameter NR_DCU,
       |  parameter string DCU_NODE_STR
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
       |  initial begin
       |    $$c("char value[500];");
       |
       |    $$c("setenv(\\"L2_CFG_STR\\",", L2_CFG_STR, ".c_str(), 1);");
       |
       |    $$c("sprintf(value, \\"%d\\",", NR_PCU, ");");
       |    $$c("setenv(\\"NR_PCU\\", value, 1);");
       |
       |    $$c("sprintf(value, \\"%d\\",", NR_DCU, ");");
       |    $$c("setenv(\\"NR_DCU\\", value, 1);");
       |
       |    $$c("setenv(\\"DCU_NODE_STR\\",", DCU_NODE_STR, ".c_str(), 1);");
       |
       |    verilua_init();
       |  end
       |`else
       |  import "DPI-C" function void setenv(string name, string value, integer replace);
       |
       |  initial begin 
       |    setenv("L2_CFG_STR", L2_CFG_STR, 1);
       |    setenv("NR_PCU", $$sformatf("%d", NR_PCU), 1);
       |    setenv("NR_DCU", $$sformatf("%d", NR_DCU), 1);
       |    setenv("DCU_NODE_STR", DCU_NODE_STR, 1);
       |
       |    #1 verilua_init();
       |  end
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
