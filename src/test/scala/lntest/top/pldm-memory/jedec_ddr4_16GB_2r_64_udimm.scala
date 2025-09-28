package lntest.top

import chisel3._
import chisel3.experimental.Analog

class jedec_ddr4_16GB_2r_64_udimm(
  num_bytes: Int = 8,
  bg_width: Int = 2,
  ba_width: Int = 2,
  col_width: Int = 10,
  addr_bits: Int = 16,
  low_addr_bits: Int = 14
) extends BlackBox(Map(
  "byte_width" -> num_bytes,
  "bank_grp_width" -> bg_width,
  "bank_addr_width" -> ba_width,
  "col_addr_width" -> col_width,
  "addr_bits" -> addr_bits,
  "low_addr_bits" -> low_addr_bits
)) {
  val io = IO(new Bundle {
    val RESET_N = Input(Bool())
    val ADDR    = Input(UInt(addr_bits.W))
    val RAS_N   = Input(Bool())
    val CAS_N   = Input(Bool())
    val WE_N    = Input(Bool())
    val CS0_N   = Input(Bool())
    val CS1_N   = Input(Bool())
    val ACT_N   = Input(Bool())
    val DM_N    = Analog(num_bytes.W)        // inout
    val CK      = Input(Bool())
    val CKE     = Input(Bool())
    val CKE1    = Input(Bool())
    val BG      = Input(UInt(bg_width.W))
    val BA      = Input(UInt(ba_width.W))
    val DQ      = Analog((num_bytes * 8).W)   // inout
    val DQS     = Analog(num_bytes.W)         // inout
    val DQS_N   = Analog(num_bytes.W)         // inout
    val PAR     = Input(Bool())
    val ALERT_N = Output(Bool())
    // val TEN = Input(Bool())
    // val A17 = Input(Bool())
    // val C0 = Input(Bool())
    // val C1 = Input(Bool())
    // val C2 = Input(Bool())
    // val ODT = Input(Bool())
    // val ODT1 = Input(Bool())
    // val SCL = Input(Bool())
    // val SDA = Input(Bool())
    // val SA0 = Input(Bool())
    // val SA1 = Input(Bool())
    // val SA2 = Input(Bool())
  })
}