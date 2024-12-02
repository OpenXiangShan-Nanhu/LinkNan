package linknan.cluster.power.controller

import chisel3._
import chisel3.util._
import linknan.cluster.power.pchannel.PChannel
import zhujiang.tilelink.{TLULBundle, TilelinkParams}

class PowerControllerTop(tlParams:TilelinkParams, csu:Boolean, csuRetClkDiv:Int = 4) extends Module {
  val io =  IO(new Bundle {
    val tls = Flipped(new TLULBundle(tlParams))
    val pChnMst = new PChannel(devActiveBits, PowerMode.powerModeBits)
    val pcsmCtrl = new PcsmCtrlIO
    val powerOnState = Input(UInt(PowerMode.powerModeBits.W))
    val intr = Output(Bool())
    val blockProbe = Output(Bool())
    val deactivate = Input(Bool())
    val mode = Output(UInt(PowerMode.powerModeBits.W))
  })
  private val pcu = Module(new PowerController(tlParams))
  private val pcsm = if(csu) Module(new CsuPcsm) else Module(new CorePcsm)

  pcu.io.tls <> io.tls
  io.pChnMst <> pcu.io.dev
  io.pcsmCtrl <> pcsm.io.ctrl
  pcu.io.powerOnState := io.powerOnState
  io.intr := pcu.io.intr
  io.blockProbe := pcu.io.blockProbe
  io.mode := pcu.io.mode
  pcsm.io.cfg <> pcu.io.pcsm
  pcu.io.deactivate := io.deactivate

  if(csu) {
    val clken = RegInit(1.U(csuRetClkDiv.W))
    val isRet = pcsm.io.cfg.modestat === PowerMode.RET
    when(isRet) {
      clken := Cat(clken(0), clken) >> 1
    }.otherwise {
      clken := 1.U
    }
    io.pcsmCtrl.clkEn := pcsm.io.ctrl.clkEn & clken(0)
  }
}
