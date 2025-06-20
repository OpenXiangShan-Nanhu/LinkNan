package linknan.utils

import chisel3._

class BitSynchronizer(stage:Int = 3, init:Option[Bool] = None) extends Module {
  val io = IO(new Bundle {
    val in = Input(Bool())
    val out = Output(Bool())
  })
  private val sync_regs = if(init.isDefined) {
    Seq.fill(stage)(RegInit(init.get))
  } else {
    Seq.fill(stage)(Reg(Bool()))
  }
  private def conn(a:Bool, b:Bool): Bool = {
    a := b
    a
  }
  sync_regs.foldRight(io.in)(conn)
  sync_regs.zipWithIndex.foreach({case(s, i) => s.suggestName(s"sync_$i")})
  io.out := sync_regs.head
}

object BitSynchronizer {
  def apply(in:Bool, stage:Int, init:Bool): Bool = {
    val sync = Module(new BitSynchronizer(stage, Some(init)))
    sync.io.in := in
    sync.io.out
  }

  def apply(in:Bool, stage:Int): Bool = {
    val sync = Module(new BitSynchronizer(stage, None))
    sync.io.in := in
    sync.io.out
  }

  def apply(in:Bool): Bool = apply(in, 3)

  def apply(in:Bool, init:Bool):Bool = apply(in, 3, init)

}