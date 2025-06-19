package linknan.cluster.power.controller

import chisel3._
import chisel3.util._
import linknan.cluster.power.pchannel._
import zhujiang.tilelink.{BaseTLULPeripheral, TLULBundle, TilelinkParams}

class PolicyBundle extends Bundle {
  val rsvd1 = UInt((24 - 1).W)
  val dynamicEn = Bool()
  val rsvd0 = UInt((8 - PowerMode.powerModeBits).W)
  val powerPolicy = UInt(PowerMode.powerModeBits.W)
  def readMask:UInt = Cat(1.U(24.W), Fill(PowerMode.powerModeBits, true.B).asTypeOf(UInt(8.W)))
  def writeMask:UInt = readMask
}

class StateBundle extends Bundle {
  val rsvd = UInt(15.W)
  val active = Bool()
  val pcsmMode = UInt(8.W)
  val devMode = UInt(8.W)
  def readMask:UInt = Cat(0.U(15.W), Fill(17, true.B))
  def writeMask:UInt = 0.U(32.W)
}

class IntrMaskBundle extends Bundle {
  val rsvd = UInt(29.W)
  val staEventIrqMask = Bool()
  val dynDenyIrqMask = Bool()
  val dynAcptIrqMask = Bool()
  def readMask:UInt = Fill(3, true.B).asTypeOf(UInt(32.W))
  def writeMask:UInt = readMask
}

class PwrCtlBundle extends Bundle {
  val dynamicEn = Output(Bool())
  val powerPolicy = Output(UInt(PowerMode.powerModeBits.W))
  val pcsmMode = Input(UInt(8.W))
  val devMode = Input(UInt(8.W))
  val active = Input(Bool())
  val transResp = Input(Valid(Bool()))
}

class PwrCtlIntrExtIO extends Bundle {
  val powerOnState = Input(UInt(PowerMode.powerModeBits.W))
  val intr = Output(Bool())
}

class PwrCtlTlIntf(tlParams:TilelinkParams) extends BaseTLULPeripheral(tlParams) {
  override def resetType: Module.ResetType.Type = Module.ResetType.Synchronous
  val addrBits = 12
  val ctl = IO(new PwrCtlBundle)
  val ext = IO(new PwrCtlIntrExtIO)
  private val policyInit = Wire(new PolicyBundle)
  policyInit := DontCare
  policyInit.powerPolicy := ext.powerOnState
  policyInit.dynamicEn := false.B

  private val policyReg = RegInit(policyInit)
  private val stateWire = WireInit(0.U.asTypeOf(new StateBundle))
  stateWire.pcsmMode := ctl.pcsmMode
  stateWire.devMode := ctl.devMode
  stateWire.active := ctl.active
  private val irqMaskReg = RegInit(Fill(3, true.B).asTypeOf(new IntrMaskBundle))
  private val irqPendingReg = RegInit(0.U.asTypeOf(new IntrMaskBundle))

  val regSeq = Seq(
    ("PWPR", policyReg, policyReg, 0x0, Some(policyReg.writeMask), Some(policyReg.readMask)),
    ("PWSR", stateWire, WireInit(0.U.asTypeOf(new StateBundle)), 0x4, Some(stateWire.writeMask), Some(stateWire.readMask)),
    ("IMR", irqMaskReg, irqMaskReg, 0x8, Some(irqMaskReg.writeMask), Some(irqMaskReg.readMask)),
    ("IPR", irqPendingReg, irqPendingReg, 0xc, Some(irqPendingReg.writeMask), Some(irqPendingReg.readMask))
  )
  private val wmap = genWriteMap()
  policyReg.rsvd0 := 0.U
  policyReg.rsvd1 := 0.U
  irqMaskReg.rsvd := 0.U
  irqPendingReg.rsvd := 0.U

  ctl.powerPolicy := policyReg.powerPolicy
  ctl.dynamicEn := policyReg.dynamicEn

  when(ctl.transResp.valid) {
    irqPendingReg.staEventIrqMask := Mux(policyReg.dynamicEn, false.B, !irqMaskReg.staEventIrqMask)
    irqPendingReg.dynDenyIrqMask := Mux(policyReg.dynamicEn, !irqMaskReg.dynDenyIrqMask & !ctl.transResp.bits, false.B)
    irqPendingReg.dynAcptIrqMask := Mux(policyReg.dynamicEn, !irqMaskReg.dynAcptIrqMask & ctl.transResp.bits, false.B)
    policyReg.powerPolicy := Mux(policyReg.dynamicEn | ctl.transResp.bits, policyReg.powerPolicy, ctl.devMode)
  }
  ext.intr := RegNext((irqPendingReg.asUInt & irqMaskReg.asUInt).orR, false.B)
}

class PowerController(tlParams:TilelinkParams) extends Module {
  private val N = devActiveBits
  private val M = PowerMode.powerModeBits
  val io =  IO(new Bundle {
    val tls = Flipped(new TLULBundle(tlParams))
    val pcsm = new PcsmPChannel
    val dev = new PChannel(devActiveBits, PowerMode.powerModeBits)
    val powerOnState = Input(UInt(PowerMode.powerModeBits.W))
    val intr = Output(Bool())
    val deactivate = Input(Bool())
    val blockReq = Output(Bool())
    val mode = Output(UInt(PowerMode.powerModeBits.W))
  })
  private val pwrOnRstReg = RegInit(3.U(2.W))
  pwrOnRstReg := Cat(false.B, pwrOnRstReg) >> 1
  private val pcsmMst = Module(new PChannelMst(N, M))
  private val devMst = Module(new PChannelMst(N, M))
  private val tlSlv = withReset(pwrOnRstReg(0)) { Module(new PwrCtlTlIntf(tlParams)) }

  private val stateBits = 15
  private def genState(idx:Int) = ((1 << idx).U(stateBits.W), idx)
  private val (sIdle, idleBit) = genState(0)
  private val (sUpHold, upHoldBit) = genState(1)
  private val (sDnHold, dnHoldBit) = genState(2)
  private val (sUpPcsm, upUpPcsmBit) = genState(3)
  private val (sUpWaitPcsm, upUpWaitPcsmBit) = genState(4)
  private val (sUpDev, upDevBit) = genState(5)
  private val (sUpWaitDev, upWaitDevBit) = genState(6)
  private val (sDnDev, dnDevBit) = genState(7)
  private val (sDnWaitDev, dnWaitDevBit) = genState(8)
  private val (sDnPcsm, dnPcsmBit) = genState(9)
  private val (sDnWaitPcsm, dnWaitPcsmBit) = genState(10)
  private val (sRestorePcsm, restorePcsmBit) = genState(11)
  private val (sWaitRestorePcsm, waitRestorePcsmBit) = genState(12)
  private val (sDeny, denyBit) = genState(13)
  private val (sComp, compBit) = genState(14)
  private val fsm = RegInit(sIdle)

  private val policyMask = UIntToOH(tlSlv.ctl.powerPolicy, devActiveBits)
  private val effectiveActive = policyMask | devMst.io.active
  private val nextDynMode = Wire(UInt(PowerMode.powerModeBits.W))
  nextDynMode := PriorityMux(effectiveActive.asBools.zipWithIndex.map(e => (e._1, e._2.U)).reverse)
  private val nextMode = Mux(tlSlv.ctl.dynamicEn, nextDynMode, tlSlv.ctl.powerPolicy)
  private val currentMode = RegInit(PowerMode.OFF)
  private val modeUpdate = currentMode =/= nextMode && fsm(idleBit)
  private val modeUpdateReg = RegNext(modeUpdate, false.B)
  private val nextModeReg = RegEnable(nextMode, modeUpdate)
  dontTouch(nextDynMode)
  when(tlSlv.ctl.dynamicEn) {
    assert(nextDynMode >= tlSlv.ctl.powerPolicy)
  }

  tlSlv.tls <> io.tls
  tlSlv.tls.a.valid := io.tls.a.valid & fsm(idleBit)
  io.tls.a.ready := tlSlv.tls.a.ready & fsm(idleBit)
  io.intr := tlSlv.ext.intr
  tlSlv.ext.powerOnState := io.powerOnState
  io.pcsm.state := pcsmMst.io.p.state
  io.pcsm.req := pcsmMst.io.p.req
  pcsmMst.io.p.accept := io.pcsm.accept
  pcsmMst.io.p.active := DontCare
  pcsmMst.io.p.deny := false.B
  io.dev <> devMst.io.p
  tlSlv.ctl.transResp.valid := fsm(denyBit) | fsm(compBit) | modeUpdateReg && io.deactivate
  tlSlv.ctl.transResp.bits := fsm(compBit)
  pcsmMst.io.defaultPState := PowerMode.OFF
  devMst.io.defaultPState := PowerMode.OFF

  io.mode := RegNext(currentMode)

  when(fsm(compBit)) {
    currentMode := nextModeReg
  }
  io.blockReq := !fsm(idleBit) && nextModeReg === PowerMode.RET || currentMode < PowerMode.ON

  pcsmMst.io.req.valid := fsm(upUpPcsmBit) | fsm(dnPcsmBit) | fsm(restorePcsmBit)
  pcsmMst.io.req.bits := Mux(fsm(restorePcsmBit), currentMode, nextModeReg)
  devMst.io.req.valid := fsm(upDevBit) | fsm(dnDevBit)
  devMst.io.req.bits := nextModeReg
  tlSlv.ctl.devMode := currentMode
  tlSlv.ctl.pcsmMode := RegNext(io.pcsm.modestat)
  tlSlv.ctl.active := RegNext(io.dev.active(N - 1))

  private val upgrade = nextModeReg > currentMode
  private val fsmNext = WireInit(fsm)
  private val holdCnt = Reg(UInt(3.W))
  when(modeUpdateReg && !io.deactivate) {
    holdCnt := Fill(holdCnt.getWidth, true.B)
  }.elsewhen(holdCnt.orR) {
    holdCnt := holdCnt - 1.U
  }

  assert(PopCount(fsm) === 1.U, cf"Illegal state $fsm%x!")
  when(modeUpdateReg && !io.deactivate || !fsm(idleBit)) {
    fsm := fsmNext
  }
  switch(fsm) {
    is(sIdle) {
      fsmNext := Mux(upgrade, sUpHold, sDnHold)
    }

    is(sUpHold) {
      fsmNext := Mux(holdCnt === 0.U, sUpPcsm, fsm)
    }
    is(sUpPcsm) {
      fsmNext := Mux(pcsmMst.io.req.ready, sUpWaitPcsm, fsm)
    }
    is(sUpWaitPcsm) {
      fsmNext := Mux(pcsmMst.io.resp.valid, sUpDev, fsm)
    }
    is(sUpDev) {
      fsmNext := Mux(devMst.io.req.ready, sUpWaitDev, fsm)
    }
    is(sUpWaitDev) {
      fsmNext := Mux(devMst.io.resp.valid, Mux(devMst.io.resp.bits, sComp, sRestorePcsm), fsm)
    }

    is(sDnHold) {
      fsmNext := Mux(holdCnt === 0.U, sDnDev, fsm)
    }
    is(sDnDev) {
      fsmNext := Mux(devMst.io.req.ready, sDnWaitDev, fsm)
    }
    is(sDnWaitDev) {
      fsmNext := Mux(devMst.io.resp.valid, Mux(devMst.io.resp.bits, sDnPcsm, sDeny), fsm)
    }
    is(sDnPcsm) {
      fsmNext := Mux(pcsmMst.io.req.ready, sDnWaitPcsm, fsm)
    }
    is(sDnWaitPcsm) {
      fsmNext := Mux(pcsmMst.io.resp.valid, sComp, fsm)
    }

    is(sRestorePcsm) {
      fsmNext := Mux(pcsmMst.io.req.ready, sWaitRestorePcsm, fsm)
    }
    is(sWaitRestorePcsm) {
      fsmNext := Mux(pcsmMst.io.resp.valid, sDeny, fsm)
    }
    is(sDeny) {
      fsmNext := sIdle
    }
    is(sComp) {
      fsmNext := sIdle
    }
  }
}
