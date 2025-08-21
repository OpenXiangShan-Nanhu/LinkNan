package linknan.utils

import chisel3._
import chisel3.util._

class SchmittDelayer(threshold:Int) extends Module {
  val io = IO(new Bundle{
    val in = Input(Bool())
    val out = Output(Bool())
  })
  private val cnt = RegInit(0.U(log2Ceil(threshold + 1).max(8).W))
  when(io.in) {
    cnt := threshold.U
  }.elsewhen(cnt =/= 0.U) {
    cnt := cnt - 1.U
  }
  io.out := cnt.orR
}

object SchmittDelayer {
  def apply(in:Bool, threshold: Int): Bool = {
    val sd = Module(new SchmittDelayer(threshold))
    sd.io.in := in
    sd.io.out
  }
}