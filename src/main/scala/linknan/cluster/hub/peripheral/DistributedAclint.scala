package linknan.cluster.hub.peripheral

import chisel3._
import chisel3.util._
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import zhujiang.{HasZJParams, ZJModule}
import zhujiang.chi.DeviceReqAddrBundle
import zhujiang.tilelink.{BaseTLULPeripheral, TilelinkParams}

class DistributedAclint(tlParams: TilelinkParams)(implicit val p: Parameters) extends BaseTLULPeripheral(tlParams) with HasZJParams {
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

  private val mtimeWrVec = WireInit(VecInit(Seq.fill(dw / 64)(mtime)))
  private val mtcmpWrVec = WireInit(VecInit(Seq.fill(dw / 64)(mtimecmp)))
  private val msipWrVec = WireInit(VecInit(Seq.fill(dw / 32)(msip)))
  private val ssipWrVec = WireInit(VecInit(Seq.fill(dw / 32)(ssip)))

  private var base = 0x0
  private def genRegSeq(reg:UInt, regWrVec:Vec[UInt], name:String, wmask:Option[UInt], rmask:Option[UInt]): Seq[(String, UInt, UInt, Int, Option[UInt], Option[UInt])] = {
    regWrVec.zipWithIndex.map({case(w, i) =>
      base = base + w.getWidth / 8
      (s"${name}_$i", reg, w, base - w.getWidth / 8, wmask, rmask)
    })
  }
  private val mtimeSeq = genRegSeq(mtime, mtimeWrVec, "mtime", None, None)
  private val mtcmpSeq = genRegSeq(mtimecmp, mtcmpWrVec, "mtcmp", None, None)
  private val msipSeq = genRegSeq(msip, msipWrVec, "msip", Some(1.U(32.W)), Some(1.U(32.W)))
  private val ssipSeq = genRegSeq(ssip, ssipWrVec, "ssip", Some(1.U(32.W)), Some(1.U(32.W)))

  val regSeq = mtimeSeq ++ mtcmpSeq ++ msipSeq ++ ssipSeq
  private val updateMap = genWriteMap()

  private def genWrite(reg:UInt, regSeq:Seq[(String, UInt, UInt, Int, Option[UInt], Option[UInt])]):Unit = {
    val updSeq = regSeq.map(m => (updateMap(m._1), m._3))
    when(Cat(updSeq.map(_._1)).orR) {
      reg := Mux1H(updSeq)
    }
  }
  genWrite(mtime, mtimeSeq)
  genWrite(mtimecmp, mtcmpSeq)
  genWrite(msip, msipSeq)
  genWrite(ssip, ssipSeq)

  io.msip := RegNext(msip(0))
  io.ssip := RegNext(ssip(0))
  io.mtip := RegNext(mtimecmp <= mtime)
}

abstract class AddrRemapper(implicit p:Parameters) extends ZJModule {
  val lnP = p(LinkNanParamsKey)
  val io = IO(new Bundle{
    val in = Input(UInt(raw.W))
    val out = Output(UInt(raw.W))
  })
  private val dwOff = log2Ceil(dw / 8)
  private val offsetInDw = io.in(dwOff - 1, 0)
  val mtimeRmp = 0x2000.U | (0x0 << dwOff).U | offsetInDw
  val mtcmpRmp = 0x2000.U | (0x1 << dwOff).U | offsetInDw
  val msipRmp = 0x2000.U | (0x2 << dwOff).U | offsetInDw
  val ssipRmp = 0x2000.U | (0x3 << dwOff).U | offsetInDw
}

class ClintAddrRemapper(implicit p:Parameters) extends AddrRemapper {
  private val spaceBits = 16
  private val clintBase = lnP.clintBase.U(raw.W)
  private val mtimecmpBase = 0x4000
  private val regAddr = io.in(spaceBits - 1, 0)
  private val devAddr = io.in(raw - 1, spaceBits)

  private val accessClint = devAddr === clintBase(raw - 1, spaceBits)
  private val accessMsip = regAddr < mtimecmpBase.U
  private val accessMtimer = regAddr === 0xBFF8.U
  private val msipCoreId = regAddr(spaceBits - 1, log2Ceil(4))
  private val mtimecmpCoreId = (regAddr - mtimecmpBase.U)(spaceBits - 1, log2Ceil(8))

  private val out = Wire(new DeviceReqAddrBundle)
  when(accessClint) {
    when(accessMsip) {
      out.ci := 0.U
      out.tag := 0.U
      out.core := msipCoreId
      out.dev := msipRmp
    }.elsewhen(accessMtimer){
      out.ci := 0.U
      out.tag := 0.U
      out.core := 0.U
      out.dev := mtimeRmp
    }.otherwise {
      out.ci := 0.U
      out.tag := 0.U
      out.core := mtimecmpCoreId
      out.dev := mtcmpRmp
    }
  }.otherwise {
    out := io.in.asTypeOf(out)
  }
  io.out := out.asUInt
}

class AclintAddrRemapper(implicit p:Parameters) extends AddrRemapper {
  private val mswiBase = lnP.mswiBase.U(raw.W)
  private val sswiBase = lnP.sswiBase.U(raw.W)

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
    out.dev := mtimeRmp
  }.elsewhen(accessMswi) {
    out.ci := accessCi
    out.tag := 0.U
    out.core := accessCore
    out.dev := msipRmp
  }.elsewhen(accessSswi) {
    out.ci := accessCi
    out.tag := 0.U
    out.core := accessCore
    out.dev := ssipRmp
  }.otherwise {
    out := io.in.asTypeOf(out)
  }
  io.out := out.asUInt
}

object AclintAddrRemapper {
  def apply(addr: UInt)(implicit p:Parameters): UInt = {
    val remapper = if(p(LinkNanParamsKey).useClint) Module(new ClintAddrRemapper) else Module(new AclintAddrRemapper)
    remapper.io.in := addr
    remapper.io.out
  }
}