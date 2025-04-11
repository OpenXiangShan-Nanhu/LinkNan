package linknan.cluster.hub.peripheral

import chisel3._
import chisel3.util._
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJModule
import zhujiang.chi.DeviceReqAddrBundle
import zhujiang.tilelink.{BaseTLULPeripheral, TilelinkParams}

class DistributedAclint(tlParams: TilelinkParams)(implicit p: Parameters) extends BaseTLULPeripheral(tlParams) {
  val io = IO(new Bundle {
    val msip = Output(Bool())
    val ssip = Output(Bool())
    val mtip = Output(Bool())
    val rtc = Input(Bool())
    val timerUpdate = Output(Valid(UInt(64.W)))
  })
  val addrBits = 12
  private val msip = RegInit(0.U(32.W))
  private val ssip = RegInit(0.U(32.W))
  private val mtimecmp = RegInit(-1.S(64.W).asUInt)
  private val mtime = RegInit(0.U(64.W))

  private val rtcSampler = Reg(UInt(2.W))
  private val rtcPosedge = rtcSampler === 1.U
  private val timerUpdate = Wire(Valid(UInt(64.W)))
  rtcSampler := Cat(rtcSampler, io.rtc)(1, 0)
  when (rtcPosedge) { mtime := mtime + 1.U }
  timerUpdate.valid := RegNext(rtcPosedge)
  timerUpdate.bits := mtime
  io.timerUpdate := Pipe(timerUpdate)

  val regSeq = Seq(
    ("mtimecmp", mtimecmp, mtimecmp, 0x0, None, None),
    ("mtime", mtime, mtime, 0x8, None, None),
    ("msip", msip, msip, 0x10, Some(0x1.U), Some(0x1.U)),
    ("ssip", ssip, ssip, 0x14, Some(0x1.U), Some(0x1.U)),
  )

  io.msip := RegNext(msip(0))
  io.ssip := RegNext(ssip(0))
  io.mtip := RegNext(mtimecmp <= mtime)
  private val updateMap = genWriteMap()
}

class AclintAddrRemapper(implicit p:Parameters) extends ZJModule {
  private val lnP = p(LinkNanParamsKey)
  private val mswiBase = lnP.mswiBase.U(raw.W)
  private val sswiBase = lnP.sswiBase.U(raw.W)

  val io = IO(new Bundle{
    val in = Input(UInt(raw.W))
    val out = Output(UInt(raw.W))
  })
  private val spaceBits = 14
  private val accessRefTimer = io.in(raw - 1, 4) === lnP.refTimerBase.U(raw.W)(raw - 1, 4)
  private val accessMswi = io.in(raw - 1, spaceBits) === mswiBase(raw - 1, spaceBits)
  private val accessSswi = io.in(raw - 1, spaceBits) === sswiBase(raw - 1, spaceBits)
  private val accessHart = io.in(clusterIdBits + 1, 2) // 32 bits each hart
  private val accessCi = accessHart.head(ciIdBits)
  private val accessCore = accessHart.tail(ciIdBits)
  private val out = Wire(new DeviceReqAddrBundle)

  when(accessRefTimer) {
    out.ci := 0.U
    out.tag := 0.U
    out.core := 0.U
    out.dev := 0x2000.U
  }.elsewhen(accessMswi) {
    out.ci := accessCi
    out.tag := 0.U
    out.core := accessCore
    out.dev := 0x2010.U
  }.elsewhen(accessSswi) {
    out.ci := accessCi
    out.tag := 0.U
    out.core := accessCore
    out.dev := 0x2014.U
  }.otherwise {
    out := io.in.asTypeOf(out)
  }
  io.out := out.asUInt
}

object AclintAddrRemapper {
  def apply(addr: UInt)(implicit p:Parameters): UInt = {
    val remapper = Module(new AclintAddrRemapper)
    remapper.io.in := addr
    remapper.io.out
  }
}