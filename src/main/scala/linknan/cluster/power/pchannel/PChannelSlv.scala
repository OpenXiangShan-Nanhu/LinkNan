package linknan.cluster.power.pchannel

import chisel3._
import chisel3.util._
import PchannelState._
import linknan.cluster.power.controller.PowerMode
import xs.utils.RegNextN

class PChannelSlv(N:Int, M:Int) extends Module {
  val io = IO(new Bundle {
    val p = Flipped(new PChannel(N, M))
    val req = Output(Valid(UInt(M.W)))
    val resp = Input(Valid(Bool()))
    val active = Input(UInt(N.W))
    val mode = Output(UInt(M.W))
  })
  private val resetDone = RegInit(false.B)
  private val fsm = RegInit(sReset)
  private val fsmNext = WireInit(fsm)
  private val rstCnt = RegInit(7.U(3.W))
  private val pactive = RegNext(io.active)
  private val preq = Some(RegNextN(io.p.req, 2)).get
  private val pstate = RegEnable(io.p.state, preq | fsm === sReset)
  private val paccept = RegNext(fsm === sAccept && resetDone)
  private val pdenied = RegNext(fsm === sDenied && resetDone)

  io.p.active := pactive
  io.p.accept := paccept
  io.p.deny := pdenied

  private val curMode = RegEnable(pstate, PowerMode.OFF, fsm === sComplete)
  private val respValid = RegInit(false.B)
  private val respAccept = Reg(Bool())

  io.mode := curMode

  when(fsm === sRequest && io.resp.valid) {
    respValid := true.B
    respAccept := io.resp.bits
  }.otherwise {
    respValid := false.B
  }

  when(respValid) {
    resetDone := true.B
  }

  io.req.valid := RegNext(fsmNext === sRequest && fsm =/= sRequest)
  io.req.bits := pstate

  when(rstCnt.orR) {
    rstCnt := rstCnt - 1.U
  }

  when(preq || fsm =/= sStable0) {
    fsm := fsmNext
  }
  switch(fsm) {
    is(sReset) {
      fsmNext := Mux(rstCnt === 0.U, sRequest, fsm)
    }
    is(sStable0) {
      fsmNext := sRequest
    }
    is(sRequest) {
      fsmNext := Mux(respValid, Mux(respAccept, sAccept, sDenied), fsm)
    }
    is(sAccept) {
      fsmNext := Mux(preq, fsm, sComplete)
    }
    is(sDenied) {
      fsmNext := Mux(preq, fsm, sContinue)
    }
    is(sComplete) {
      fsmNext := sStable0
    }
    is(sContinue) {
      fsmNext := sStable0
    }
  }
}
