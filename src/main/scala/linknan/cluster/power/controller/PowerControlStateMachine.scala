package linknan.cluster.power.controller

import chisel3._
import chisel3.util._
import linknan.cluster.power.pchannel.PChannelSlv
import xs.utils.RegNextN

class PcsmCtrlVec extends Bundle {
  val pwrEn = Bool()
  val ckEn = Bool()
  val sigEn = Bool()
  val fnEn = Bool()
}

class PcsmPChannel extends Bundle {
  val state = Output(UInt(PowerMode.powerModeBits.W))
  val req = Output(Bool())
  val accept = Input(Bool())
  val modestat = Input(UInt(PowerMode.powerModeBits.W))
}

class PcsmCtrlIO extends Bundle {
  val pwrReq = Output(Bool())
  val pwrResp = Input(Bool())
  val clkEn = Output(Bool())
  val isoEn = Output(Bool())
  val reset = Output(Bool())
}

class PcsmCtrlDriver(isoDelay:Int = 16, clkEnDelay:Int = 64, rstDelay:Int = 16) extends Module {
  val io = IO(new Bundle {
    val req = Input(Valid(new Bundle{
      val ctrlVec = new PcsmCtrlVec
      val up = Bool()
    }))
    val resp = Output(Bool())
    val ctrl = new PcsmCtrlIO
  })
  private val maxDelay = Seq(isoDelay, clkEnDelay, rstDelay).max
  private val stateBits = 10
  private def genState(idx:Int):(UInt, Int) = ((1 << idx).U(stateBits.W), idx)

  private val (sIdle, idleBit) = genState(0)
  private val (sUpPwr, upPwrBit) = genState(1)
  private val (sUpSig, upSigBit) = genState(2)
  private val (sUpCk, upCkBit) = genState(3)
  private val (sUpFn, upFnBit) = genState(4)
  private val (sDnFn, dnFnBit) = genState(5)
  private val (sDnCk, dnCkBit) = genState(6)
  private val (sDnSig, dnSigBit) = genState(7)
  private val (sDnPwr, dnPwrBit) = genState(8)
  private val (sComp, compBit) = genState(9)

  private val fsm = RegInit(sIdle)
  private val fsmNext = WireInit(fsm)
  private val reqFire = fsm === sIdle && io.req.valid
  private val reqValid = RegNext(reqFire, false.B)
  private val reqCtrl = RegEnable(io.req.bits.ctrlVec, reqFire)
  private val reqUp = RegEnable(io.req.bits.up, reqFire)
  private val ctrlState = RegInit(0.U.asTypeOf(new PcsmCtrlVec))
  private val cnt = Reg(UInt(log2Ceil(maxDelay + 1).W))
  private val pwrResp = Some(RegNextN(io.ctrl.pwrResp, 2)).get
  dontTouch(fsmNext)

  private val doPwr = fsm(upPwrBit) | fsm(dnPwrBit)
  private val doIso = fsm(upSigBit) | fsm(dnSigBit)
  private val doClk = fsm(upCkBit)  | fsm(dnCkBit)
  private val doRst = fsm(upFnBit)  | fsm(dnFnBit)
  ctrlState.pwrEn := Mux(doPwr, reqCtrl.pwrEn, ctrlState.pwrEn)
  ctrlState.sigEn := Mux(doIso, reqCtrl.sigEn, ctrlState.sigEn)
  ctrlState.ckEn := Mux(doClk, reqCtrl.ckEn, ctrlState.ckEn)
  ctrlState.fnEn := Mux(doRst, reqCtrl.fnEn, ctrlState.fnEn)

  io.ctrl.pwrReq := ctrlState.pwrEn
  io.ctrl.isoEn := !ctrlState.sigEn
  io.ctrl.clkEn := ctrlState.ckEn
  io.ctrl.reset := !ctrlState.fnEn

  private val cntLoadCondSeq = Seq(upCkBit, upSigBit, upFnBit, dnFnBit, dnSigBit, dnCkBit)
  private val cntLoadCond = cntLoadCondSeq.map(c => !fsm(c) && fsmNext(c)).reduce(_ || _)

  when(cntLoadCond) {
    cnt := MuxCase(0.U, Seq(
      doIso -> isoDelay.U,
      doClk -> clkEnDelay.U,
      doRst -> rstDelay.U
    ))
  }.elsewhen(cnt.orR){
    cnt := cnt - 1.U
  }

  io.resp := fsm === sComp

  private val sigNotChange = RegEnable(io.req.bits.ctrlVec.sigEn === ctrlState.sigEn, reqFire)
  private val ckNotChange = RegEnable(io.req.bits.ctrlVec.ckEn === ctrlState.ckEn, reqFire)
  private val fnNotChange = RegEnable(io.req.bits.ctrlVec.fnEn === ctrlState.fnEn, reqFire)
  private val pwrDone = pwrResp === ctrlState.pwrEn
  private val sigDone = Mux(sigNotChange, true.B, cnt === 0.U)
  private val ckDone = Mux(ckNotChange, true.B, cnt === 0.U)
  private val fnDone = Mux(fnNotChange, true.B, cnt === 0.U)

  assert(PopCount(fsm) === 1.U, cf"Illegal state $fsm%x!")
  when(reqValid || !fsm(idleBit)) {
    fsm := fsmNext
  }
  switch(fsm) {
    is(sIdle) {
      fsmNext := Mux(reqUp, sUpPwr, sDnFn)
    }
    is(sUpPwr) {
      fsmNext := Mux(pwrDone, sUpSig, fsm)
    }
    is(sUpSig) {
      fsmNext := Mux(sigDone, sUpCk, fsm)
    }
    is(sUpCk) {
      fsmNext := Mux(ckDone, sUpFn, fsm)
    }
    is(sUpFn) {
      fsmNext := Mux(fnDone, sComp, fsm)
    }
    is(sDnFn) {
      fsmNext := Mux(fnDone, sDnCk, fsm)
    }
    is(sDnCk) {
      fsmNext := Mux(ckDone, sDnSig, fsm)
    }
    is(sDnSig) {
      fsmNext := Mux(sigDone, sDnPwr, fsm)
    }
    is(sDnPwr) {
      fsmNext := Mux(pwrDone, sComp, fsm)
    }
    is(sComp) {
      fsmNext := sIdle
    }
  }
}

abstract class PowerControlStateMachine extends Module {
  val io = IO(new Bundle{
    val cfg = Flipped(new PcsmPChannel)
    val ctrl = new PcsmCtrlIO
  })
  private val pSlv = Module(new PChannelSlv(0, PowerMode.powerModeBits))
  private val ctrl = Module(new PcsmCtrlDriver)
  private val nextMode = RegEnable(pSlv.io.req.bits, pSlv.io.req.valid)
  private val modeUpdate = RegNext(pSlv.io.req.valid, false.B)
  private val pcsmMode = RegEnable(nextMode, PowerMode.OFF, ctrl.io.resp)

  pSlv.io.p.state := io.cfg.state
  pSlv.io.p.req := io.cfg.req
  io.cfg.accept := pSlv.io.p.accept

  def PwrEnMode:UInt
  def ClkEnMode:UInt
  def SigEnMode:UInt
  def FnEnMode:UInt
  ctrl.io.req.valid := modeUpdate && nextMode =/= pcsmMode
  ctrl.io.req.bits.up := nextMode > pcsmMode
  ctrl.io.req.bits.ctrlVec.pwrEn := nextMode > PwrEnMode
  ctrl.io.req.bits.ctrlVec.ckEn := nextMode > ClkEnMode
  ctrl.io.req.bits.ctrlVec.sigEn := nextMode > SigEnMode
  ctrl.io.req.bits.ctrlVec.fnEn := nextMode > FnEnMode

  pSlv.io.resp.valid := modeUpdate && nextMode === pcsmMode || ctrl.io.resp
  pSlv.io.resp.bits := true.B
  pSlv.io.active := DontCare
  io.ctrl <> ctrl.io.ctrl
  io.cfg.modestat := pcsmMode
}

class CorePcsm extends PowerControlStateMachine {
  final override def PwrEnMode:UInt = PowerMode.OFF
  final override def ClkEnMode = PowerMode.RET
  final override def SigEnMode = PowerMode.RET
  final override def FnEnMode = PowerMode.OFF
}

class CsuPcsm extends PowerControlStateMachine {
  final override def PwrEnMode = PowerMode.OFF
  final override def ClkEnMode = PowerMode.OFF
  final override def SigEnMode = PowerMode.OFF
  final override def FnEnMode = PowerMode.OFF
}