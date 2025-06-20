package linknan.cluster.power.pchannel

import chisel3._
import chisel3.util._
import PchannelState._
import linknan.cluster.power.controller.PowerMode
import linknan.utils.AsyncBusSink

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
  private val pactive = RegNext(io.active, 0.U)
  private val paccept = RegNext(fsm === sAccept && resetDone)
  private val pdenied = RegNext(fsm === sDenied && resetDone)

  private val asyncSink = Module(new AsyncBusSink(UInt(io.p.state.getWidth.W)))
  asyncSink.io.in.valid := io.p.req
  asyncSink.io.in.bits := io.p.state
  asyncSink.io.en := fsm === sReset
  private val preq = asyncSink.io.out.valid
  private val pstate = asyncSink.io.out.bits

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

  when(fsm =/= fsmNext) {
    fsm := fsmNext
  }
  switch(fsm) {
    is(sReset) {
      fsmNext := Mux(rstCnt === 0.U, sRequest, fsm)
    }
    is(sStable0) {
      fsmNext := Mux(preq, sRequest, fsm)
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
