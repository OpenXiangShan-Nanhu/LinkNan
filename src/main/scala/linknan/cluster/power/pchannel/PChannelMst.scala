package linknan.cluster.power.pchannel

import chisel3._
import chisel3.util._
import PchannelState._
import linknan.utils.AsyncBusSource
import xs.utils.RegNextN

class PChannelMst(N:Int, M:Int, tinit:Int = 64, preTs:Int = 127, postTs:Int = 127) extends Module {
  val io = IO(new Bundle {
    val p = new PChannel(N, M)
    val req = Flipped(Decoupled(UInt(M.W)))
    val resp = Output(Valid(Bool()))
    val defaultPState = Input(UInt(M.W))
    val active = Output(UInt(N.W))
  })
  private val fsm = RegInit(sReset)
  private val fsmNext = WireInit(fsm)
  private val counterMax = Seq(tinit, preTs, postTs).max
  private val utilCnt = RegInit((tinit - 1).U(log2Ceil(counterMax).W))
  private val pactive = Some(RegNextN(io.p.active, 2)).get
  private val pstate = Reg(UInt(M.W))
  private val preq = RegInit(false.B)
  private val paccept = Some(RegNextN(io.p.accept, 2)).get
  private val pdenied = Some(RegNextN(io.p.deny, 2)).get
  private val _reset = RegNext(false.B, true.B)

  private val asyncSrc = Module(new AsyncBusSource(UInt(pstate.getWidth.W)))
  asyncSrc.io.in.valid := preq
  asyncSrc.io.in.bits := pstate
  asyncSrc.io.en := _reset
  io.p.req := asyncSrc.io.out.valid
  io.p.state := asyncSrc.io.out.bits
  io.active := pactive

  io.req.ready := fsm === sStable0 && utilCnt === 0.U
  when(io.req.fire) {
    preq := true.B
  }.elsewhen(fsm =/= sRequest) {
    preq := false.B
  }


  when(_reset) {
    pstate := io.defaultPState
  }.elsewhen(io.req.fire) {
    pstate := io.req.bits
  }

  when(fsm =/= sStable0 && fsmNext === sStable0) {
    utilCnt := preTs.U
  }.elsewhen(fsm =/= sStable1 && fsmNext === sStable1) {
    utilCnt := postTs.U
  }.elsewhen(utilCnt.orR) {
    utilCnt := utilCnt - 1.U
  }

  io.resp.valid := fsm === sAccept || fsm === sDenied
  io.resp.bits := paccept

  fsm := fsmNext
  switch(fsm) {
    is(sReset) {
      fsmNext := Mux(utilCnt === 0.U, sStable0, fsm)
    }
    is(sStable0) {
      fsmNext := Mux(io.req.fire, sRequest, fsm)
    }
    is(sRequest) {
      fsmNext := Mux(paccept, sAccept, Mux(pdenied, sDenied, fsm))
    }
    is(sAccept) {
      fsmNext := Mux(paccept, fsm, sComplete)
    }
    is(sDenied) {
      fsmNext := Mux(pdenied, fsm, sContinue)
    }
    is(sComplete) {
      fsmNext := sStable1
    }
    is(sContinue) {
      fsmNext := sStable1
    }
    is(sStable1) {
      fsmNext := Mux(utilCnt === 0.U, sStable0, fsm)
    }
  }
}
