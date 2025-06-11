package linknan.cluster.power.controller

import chisel3._
import chisel3.util._
import linknan.cluster.power.pchannel.{PChannel, PChannelSlv}
import xs.utils.RegNextN

class CoreStateController extends Module {
  val io = IO(new Bundle {
    val nextMode    = Input(UInt(PowerMode.powerModeBits.W))

    val timeout     = Input(Bool())

    val fence       = Input(Bool())
    val fenceFinish = Output(Bool())
    val flush       = Input(Bool())
    val flushFinish = Output(Bool())

    val flushSb     = Output(Bool())
    val sbIsEmpty   = Input(Bool())
    val fencei      = Output(Bool())
    val l2Flush     = Output(Bool())
    val l2FlushDone = Input(Bool())
  })

  private val stateBits = 9
  private def genState(idx: Int) = ((1 << idx).U(stateBits.W), idx)
  private val (sIdle, idleBit) = genState(0)
  private val (sFlushSb, flushSbBit) = genState(1)
  private val (sWaitFlushSb, waitFlushSbBit) = genState(2)
  private val (sFencei, fenceiBit) = genState(3)
  private val (sWaitFencei, waitFenceiBit) = genState(4)
  private val (sL2Flush, l2FlushBit) = genState(5)
  private val (sWaitL2Flush, waitL2FlushBit) = genState(6)
  private val (sFenceFinish, fenceFinishBit) = genState(7)
  private val (sFlushFinish, flushFinishBit) = genState(8)
  private val fsm = RegInit(sIdle)

  private val fsmNext = WireInit(fsm)
  fsm := fsmNext

  switch(fsm) {
    is(sIdle) {
      fsmNext := Mux(io.fence, sFlushSb, Mux(io.flush, sL2Flush, sIdle))
    }
    is(sFlushSb) {
      fsmNext := Mux(io.timeout, sIdle, sWaitFlushSb)
    }
    is(sWaitFlushSb) {
      fsmNext := Mux(io.timeout, sIdle, Mux(io.sbIsEmpty, sFencei, sWaitFlushSb))
    }
    is(sFencei) {
      fsmNext := Mux(io.timeout, sIdle, sWaitFencei)
    }
    is(sWaitFencei){
      fsmNext := Mux(io.timeout, sIdle, sFenceFinish)
    }
    is(sL2Flush) {
      fsmNext := Mux(io.timeout, sIdle, sWaitL2Flush)
    }
    is(sWaitL2Flush) {
      fsmNext := Mux(io.timeout, sIdle, Mux(io.l2FlushDone, sFlushFinish, sWaitL2Flush))
    }
    is(sFenceFinish) {
      fsmNext := sIdle
    }
    is(sFlushFinish) {
      fsmNext := sIdle
    }
  }

  io.flushSb := fsmNext === sFlushSb && fsm =/= sFlushSb
  io.fencei  := fsmNext === sFencei && fsm =/= sFencei
  io.l2Flush := fsmNext === sL2Flush && fsm =/= sL2Flush
  io.fenceFinish := fsm(fenceFinishBit)
  io.flushFinish := fsm(flushFinishBit)
}

class CorePowerController extends Module {
  val io = IO(new Bundle{
    val pchn      = Flipped(new PChannel(devActiveBits, PowerMode.powerModeBits))

    val cpuHalt     = Input(Bool())
    val wfiCtrRst   = Output(Bool())
    val timeout     = Input(Bool())

    val flushSb     = Output(Bool())
    val sbIsEmpty   = Input(Bool())
    val fencei      = Output(Bool())
    val l2Flush     = Output(Bool())
    val l2FlushDone = Input(Bool())
  })

  dontTouch(io)

  private val pSlv = Module(new PChannelSlv(devActiveBits, PowerMode.powerModeBits))
  private val csCtl = Module(new CoreStateController)

  private val stateBits = 7
  private def genState(idx: Int) = ((1 << idx).U(stateBits.W), idx)
  private val (sIdle, idleBit) = genState(0)
  private val (sHalt, haltBit) = genState(1)
  private val (sFence, fenceBit) = genState(2)
  private val (sFlush, flushBit) = genState(3)
  private val (sTimeout, timeoutBit) = genState(4)
  private val (sRet, retBit) = genState(5)
  private val (sOff, offBit) = genState(6)
  private val fsm = RegInit(sOff)
  private val fsmNext = WireInit(fsm)
  fsm := fsmNext

  private val cpuHalt = RegNextN(io.cpuHalt, 2)
  private val timeout = RegNextN(io.timeout, 2)
  private val nextMode = RegEnable(pSlv.io.req.bits, PowerMode.OFF, pSlv.io.req.valid)
  private val modeUpdate = RegNext(pSlv.io.req.valid, false.B)


  pSlv.io.p <> io.pchn
  pSlv.io.resp.valid := modeUpdate && fsm(idleBit) ||
    fsm(timeoutBit) ||
    fsm(retBit) && nextMode === PowerMode.RET ||
    fsm(offBit) && nextMode === PowerMode.OFF ||
    fsm(retBit) && fsmNext === sHalt ||
    fsm(offBit) && fsmNext === sIdle

  pSlv.io.resp.bits := !(modeUpdate && fsm(idleBit)) && !fsm(timeoutBit)
  pSlv.io.active := Cat(!cpuHalt, cpuHalt, true.B)
  dontTouch(pSlv.io)

  io.wfiCtrRst := fsmNext === sFence && fsm =/= sFence
  csCtl.io.nextMode := nextMode
  csCtl.io.fence := fsm(fenceBit)
  csCtl.io.flush := fsm(flushBit)
  csCtl.io.timeout := fsmNext === sTimeout && fsm =/= sTimeout
  io.flushSb := csCtl.io.flushSb
  io.fencei  := csCtl.io.fencei
  io.l2Flush := csCtl.io.l2Flush
  csCtl.io.sbIsEmpty := io.sbIsEmpty
  csCtl.io.l2FlushDone := io.l2FlushDone


  switch(fsm) {
    is(sIdle) {
      fsmNext := Mux(cpuHalt, sHalt, sIdle)
    }
    is(sHalt) {
      fsmNext := Mux(!cpuHalt, sIdle, Mux(pSlv.io.req.valid, sFence, sHalt))
    }
    is(sFence) {
      fsmNext := Mux(timeout, sTimeout,
        Mux(csCtl.io.fenceFinish, Mux(nextMode === PowerMode.RET, sRet, sFlush), sFence))
    }
    is(sFlush) {
      fsmNext := Mux(timeout, sTimeout, Mux(csCtl.io.flushFinish, sOff, sFlush))
    }
    is(sTimeout) {
      fsmNext := sIdle
    }
    is(sRet) {
      fsmNext := Mux(nextMode === PowerMode.ON, sHalt, sRet)
    }
    is(sOff) {
      fsmNext := Mux(nextMode === PowerMode.ON, sIdle, sOff)
    }
  }

}
