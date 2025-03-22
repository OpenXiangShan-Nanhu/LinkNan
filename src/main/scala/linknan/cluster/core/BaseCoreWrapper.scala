package linknan.cluster.core

import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import chisel3._
import freechips.rocketchip.resources.BindingScope
import linknan.cluster.hub.ImsicBundle
import linknan.cluster.power.controller.{PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.{PChannel, PChannelSlv}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyRawModuleImp}
import xiangshan.BusErrorUnitInfo
import xijiang.{Node, NodeType}
import xs.utils.ResetGen
import zhujiang.device.socket.{ChiPdcDevSide, DevPdcBundle}
import zhujiang.{DftWires, ZJParametersKey}

class CoreWrapperIO(node:Node)(implicit p:Parameters) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(AsyncReset())
  val chiPdc = new DevPdcBundle(node)
  val pchn = Flipped(new PChannel(devActiveBits, PowerMode.powerModeBits))
  val pwrEnReq = Input(Bool())
  val pwrEnAck = Output(Bool())
  val isoEn = Input(Bool())
  val mhartid = Input(UInt(p(ZJParametersKey).clusterIdBits.W))
  val reset_vector = Input(UInt(p(ZJParametersKey).requestAddrBits.W))
  val icacheErr = Output(new BusErrorUnitInfo)
  val dcacheErr = Output(new BusErrorUnitInfo)
  val msip = Input(Bool())
  val mtip = Input(Bool())
  val meip = Input(Bool())
  val seip = Input(Bool())
  val dbip = Input(Bool())
  val timerUpdate = Input(Valid(UInt(64.W)))
  val reset_state = Output(Bool())
  val dft = Input(new DftWires)
}

abstract class BaseCoreWrapper
  (implicit p:Parameters) extends LazyModule with BindingScope {
  def module: BaseCoreWrapperImpl
}

@instantiable
class BaseCoreWrapperImpl(outer:BaseCoreWrapper, node:Node) extends LazyRawModuleImp(outer) with ImplicitClock with ImplicitReset {
  override def provideImplicitClockToLazyChildren = true
  @public val io = IO(new CoreWrapperIO(node))
  dontTouch(io)
  childClock := io.clock
  childReset := withClockAndReset(io.clock, io.reset){ ResetGen(dft = Some(io.dft.reset)) }
  def implicitClock = childClock
  def implicitReset = childReset

  val pdc = Module(new ChiPdcDevSide(node))
  io.chiPdc <> pdc.io.dev

  val cpuHalt = Wire(Bool())
  val pSlv = Module(new PChannelSlv(devActiveBits, PowerMode.powerModeBits))
  pSlv.io.p <> io.pchn
  pSlv.io.resp.valid := pSlv.io.req.valid
  pSlv.io.resp.bits := true.B
  pSlv.io.active := Cat(!cpuHalt, cpuHalt, true.B)
  io.pwrEnAck := io.pwrEnReq
  dontTouch(io.pwrEnReq)
  dontTouch(io.pwrEnAck)
  dontTouch(io.isoEn)
  dontTouch(pSlv.io)

  io.icacheErr := DontCare
  io.dcacheErr := DontCare
  val reset_state = Wire(AsyncReset())
  reset_state := io.reset
  io.reset_state := withClockAndReset(childClock, reset_state) {
    val rstStateReg = RegInit("b111".U(3.W))
    rstStateReg := Cat(0.U(1.W), rstStateReg(2, 1))
    rstStateReg(0)
  }
}
