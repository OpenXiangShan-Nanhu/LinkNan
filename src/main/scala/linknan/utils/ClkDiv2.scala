package linknan.utils

import chisel3._
import chisel3.util._

class ClkDiv2 extends BlackBox with HasBlackBoxInline {
  override val desiredName = xs.utils.GlobalData.prefix + "ClkDiv2"
  val io = IO(new Bundle {
    val CK = Input(Clock())
    val Q = Output(Clock())
  })
  setInline(s"$desiredName.sv",
    s"""
       |module $desiredName (
       |  input  wire CK,
       |  output wire Q
       |);
       |reg div;
       |reg out;
       |`ifndef SYNTHESIS
       |  initial div = 1'b1;
       |  initial out = 1'b0;
       |`endif
       |
       |always @ (posedge clock) div <= ~div;
       |always @ (posedge clock) out <= div;
       |
       |`ifndef __VIVADO__
       |assign Q = out;
       |`else
       |assign Q = CK;
       |`endif
       |endmodule""".stripMargin)
}
