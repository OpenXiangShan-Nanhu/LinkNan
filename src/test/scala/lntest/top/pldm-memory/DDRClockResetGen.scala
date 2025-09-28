package lntest.top

import chisel3._
import chisel3.util.HasBlackBoxInline

class DDRClockResetGen extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val sys_rst_n = Input(Bool())
    val mc_init_stage1_done = Input(Bool())
    val mc_init_stage2_done = Input(Bool())
    val mc_clk = Output(Clock())
    val apb_clk = Output(Clock())
    val phy_clk = Output(Clock())
    val mc_rst_n = Output(Bool())
    val apb_rst_n = Output(Bool())
    val axi_rst_n = Output(Bool())
    val mc_init_done = Output(Bool())
  })
  setInline(
    "DDRClockResetGen.sv",
    """module DDRClockResetGen(
      |  input  logic sys_rst_n,
      |  input  logic mc_init_stage1_done,
      |  input  logic mc_init_stage2_done,
      |  output logic mc_clk,
      |  output logic apb_clk,
      |  output logic phy_clk,
      |  output logic mc_rst_n,
      |  output logic apb_rst_n,
      |  output logic axi_rst_n,
      |  output logic mc_init_done
      |);
      |   logic [7:0] cnt;
      |   wire    mc_clock;
      |   wire    apb_clock;
      |   wire    phy_clock;
      |   assign  mc_clk = mc_clock;
      |   assign  apb_clk = apb_clock;
      |   assign  phy_clk = phy_clock;
      |   always_ff @(posedge apb_clock or negedge sys_rst_n) begin
      |      if (!sys_rst_n) begin
      |         cnt <= 8'b0;
      |      end else if(cnt < 8'd128 + 8'd10) begin
      |         cnt <= cnt + 1;
      |      end
      |   end
      |   assign apb_rst_n = (cnt >= 8'd10);
      |   assign axi_rst_n = mc_init_stage1_done;
      |   assign mc_rst_n = mc_init_stage1_done;
      |   assign mc_init_done = mc_init_stage1_done & mc_init_stage2_done;
      |endmodule
    """.stripMargin
  )
}
