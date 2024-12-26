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
       |    $$c("sprintf(value, \\"%d\\",", NR_L2, ");");
       |    $$c("setenv(\\"NR_L2\\", value, 1);");
       |
       |    $$c("sprintf(value, \\"%d\\",", NR_L2_BANK, ");");
       |    $$c("setenv(\\"NR_L2_BANK\\", value, 1);");
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
       |    setenv("NR_L2", $$sformatf("%d", NR_L2), 1);
       |    setenv("NR_L2_BANK", $$sformatf("%d", NR_L2_BANK), 1);
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
