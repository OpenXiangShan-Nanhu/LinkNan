package linknan.utils

import chisel3._
import chisel3.util._

class AsyncBusSource[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val in = Input(Valid(gen))
    val out = Output(Valid(gen))
    val en = Input(Bool())
  })
  private val src_v = RegNext(io.in.valid, false.B)
  private val src_d = RegEnable(io.in.bits, io.in.valid | io.en)
  io.out.valid := src_v
  io.out.bits := src_d
}

class AsyncBusSink[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val in = Input(Valid(gen))
    val out = Output(Valid(gen))
    val en = Input(Bool())
  })
  private val vld_sync = Module(new BitSynchronizer(3))
  vld_sync.io.in := io.in.valid
  private val sink_v = vld_sync.io.out
  private val sink_d = RegEnable(io.in.bits, sink_v | io.en)
  io.out.valid := sink_v
  io.out.bits := sink_d
}