package linknan.utils

import chisel3._
import chisel3.util._

class ClkDiv2 extends BlackBox with HasBlackBoxInline {
  override val desiredName = xs.utils.GlobalData.prefix + "ClkDiv2"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val out = Output(Clock())
  })
  setInline(s"$desiredName.sv",
    s"""
       |module $desiredName (
       |  input wire clock,
       |  output reg out
       |);
       |reg div;
       |`ifndef SYNTHESIS
       |  initial div = 1'b1;
       |  initial out = 1'b1;
       |`endif
       |
       |always @ (posedge clock) div <= ~div;
       |always @ (posedge clock) out <= div;
       |endmodule""".stripMargin)
}
