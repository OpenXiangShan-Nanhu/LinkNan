package linknan.cluster.core

import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import chisel3._
import freechips.rocketchip.resources.BindingScope
import freechips.rocketchip.util.{AsyncBundle, AsyncQueueParams, AsyncQueueSink}
import linknan.cluster.power.controller.{PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.{PChannel, PChannelSlv}
import linknan.soc.LinkNanParamsKey
import linknan.utils.connectChiChn
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyRawModuleImp}
import xijiang.Node
import xs.utils.ResetGen
import xs.utils.sram.SramCtrlBundle
import zhujiang.chi.{DataFlit, RReqFlit, RespFlit, SnoopFlit}
import zhujiang.device.socket.{DeviceIcnAsyncBundle, DeviceSideAsyncModule}
import zhujiang.{DftWires, ZJParametersKey}

class CoreWrapperIO(node:Node)(implicit p:Parameters) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(AsyncReset())
  val chi = new DeviceIcnAsyncBundle(node)
  val pchn = Flipped(new PChannel(devActiveBits, PowerMode.powerModeBits))
  val pwrEnReq = Input(Bool())
  val pwrEnAck = Output(Bool())
  val isoEn = Input(Bool())
  val mhartid = Input(UInt(p(ZJParametersKey).clusterIdBits.W))
  val reset_vector = Input(UInt(p(ZJParametersKey).requestAddrBits.W))
  val msip = Input(Bool())
  val mtip = Input(Bool())
  val meip = Input(Bool())
  val seip = Input(Bool())
  val dbip = Input(Bool())
  val timer = Flipped(new AsyncBundle(UInt(64.W), p(LinkNanParamsKey).coreTimerAsyncParams))
  val reset_state = Output(Bool())
  val dft = Input(new DftWires)
  val ramctl = Input(new SramCtrlBundle)
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

  val pdc = Module(new DeviceSideAsyncModule(node))
  io.chi <> pdc.io.async
  pdc.io.icn.rx.debug.foreach(_ := DontCare)

  private val timerSink = Module(new AsyncQueueSink(UInt(64.W), p(LinkNanParamsKey).coreTimerAsyncParams))
  timerSink.io.async <> io.timer
  val timerUpdate = Wire(Valid(UInt(64.W)))
  timerUpdate.valid := timerSink.io.deq.valid
  timerUpdate.bits := timerSink.io.deq.bits
  timerSink.io.deq.ready := true.B

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

  val txreq = Wire(Decoupled(new RReqFlit))
  val txrsp = Wire(Decoupled(new RespFlit))
  val txdat = Wire(Decoupled(new DataFlit))
  val rxsnp = Wire(Decoupled(new SnoopFlit))
  val rxrsp = Wire(Decoupled(new RespFlit))
  val rxdat = Wire(Decoupled(new DataFlit))

  connectChiChn(pdc.io.icn.rx.req.get, txreq)
  connectChiChn(pdc.io.icn.rx.resp.get, txrsp)
  connectChiChn(pdc.io.icn.rx.data.get, txdat)
  connectChiChn(rxsnp, pdc.io.icn.tx.snoop.get)
  connectChiChn(rxrsp, pdc.io.icn.tx.resp.get)
  connectChiChn(rxdat, pdc.io.icn.tx.data.get)

  val reset_state = Wire(AsyncReset())
  reset_state := io.reset
  io.reset_state := withClockAndReset(childClock, reset_state) {
    val rstStateReg = RegInit("b111".U(3.W))
    rstStateReg := Cat(0.U(1.W), rstStateReg(2, 1))
    rstStateReg(0)
  }
}
