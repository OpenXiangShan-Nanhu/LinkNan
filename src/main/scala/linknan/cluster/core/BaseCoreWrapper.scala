package linknan.cluster.core

import SimpleL2.Configs.L2ParamKey
import chisel3.experimental.hierarchy.{instantiable, public}
import chisel3.util._
import chisel3._
import freechips.rocketchip.diplomacy.{AddressSet, RegionType, TransferSizes}
import freechips.rocketchip.resources.BindingScope
import freechips.rocketchip.tilelink._
import linknan.cluster.hub.ImsicBundle
import linknan.cluster.power.controller.{PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.{PChannel, PChannelSlv}
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.ValName
import org.chipsalliance.diplomacy.lazymodule.{LazyModule, LazyRawModuleImp}
import xiangshan.{BusErrorUnitInfo, HasXSParameter}
import xs.utils.ResetGen
import xs.utils.tl.TLNanhuBusKey
import zhujiang.{DftWires, ZJParametersKey}

class CoreWrapperIO(ioParams:TLBundleParameters, l2Params: TLBundleParameters)(implicit p:Parameters) extends Bundle {
  val clock = Input(Clock())
  val reset = Input(AsyncReset())
  val cio = new TLBundle(ioParams)
  val l2 = new TLBundle(l2Params)
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
  val imsic = Flipped(new ImsicBundle)
  val reset_state = Output(Bool())
  val dft = Input(new DftWires)
}

abstract class BaseCoreWrapper(legacyMmap:Boolean = true)(implicit p:Parameters) extends LazyModule with BindingScope {
  private val paddrBits = p(ZJParametersKey).requestAddrBits
  private val allSpace = AddressSet(0x0L, (0x1L << paddrBits) - 1)
  private val mmioSpace = AddressSet(0x1L << (paddrBits - 1), (0x1L << paddrBits - 1) - 1)
  private val mmioAddressSets = if(legacyMmap) {
    Seq(AddressSet(0L, 0x7FFF_FFFF), mmioSpace)
  } else {
    Seq(mmioSpace)
  }
  private val cacheAddressSets = if(legacyMmap) {
    allSpace.subtract(mmioSpace).flatMap(_.subtract(AddressSet(0L, 0x7FFF_FFFF)))
  } else {
    allSpace.subtract(mmioSpace)
  }
  private val mmioSlvParams = TLSlavePortParameters.v1(
    managers = Seq(TLSlaveParameters.v1(
      address = mmioAddressSets,
      supportsGet = TransferSizes(1, 8),
      supportsPutFull = TransferSizes(1, 8),
      supportsPutPartial = TransferSizes(1, 8),
      regionType = RegionType.GET_EFFECTS
    )),
    beatBytes = 8
  )

  private val l2param = p(L2ParamKey)
  private val l2NodeParameters = TLSlavePortParameters.v1(
    managers = Seq(
      TLSlaveParameters.v1(
        address = cacheAddressSets,
        regionType = RegionType.CACHED,
        supportsAcquireT = TransferSizes(l2param.blockBytes, l2param.blockBytes),
        supportsAcquireB = TransferSizes(l2param.blockBytes, l2param.blockBytes),
        supportsArithmetic = TransferSizes(1, l2param.beatBytes),
        supportsGet = TransferSizes(1, l2param.beatBytes),
        supportsLogical = TransferSizes(1, l2param.beatBytes),
        supportsPutFull = TransferSizes(1, l2param.beatBytes),
        supportsPutPartial = TransferSizes(1, l2param.beatBytes),
        executable = true
      )
    ),
    beatBytes = 32,
    minLatency = 2,
    responseFields = Nil,
    requestKeys = Seq(TLNanhuBusKey),
    endSinkId = 256 * (1 << log2Ceil(p(L2ParamKey).nrSlice))
  )
  val cioNode = TLManagerNode(Seq(mmioSlvParams))
  val l2Node = TLManagerNode(Seq(l2NodeParameters))
  def module: BaseCoreWrapperImpl
  def getPortParams:(TLBundleParameters, TLBundleParameters) = {
    val ioParams = cioNode.in.head._2.bundle
    val l2Params = l2Node.in.head._2.bundle
    (ioParams, l2Params)
  }
}

@instantiable
class BaseCoreWrapperImpl(outer:BaseCoreWrapper) extends LazyRawModuleImp(outer) with ImplicitClock with ImplicitReset {
  override def provideImplicitClockToLazyChildren = true
  private val (ioParams, l2Params) = outer.getPortParams
  @public val io = IO(new CoreWrapperIO(ioParams, l2Params))
  dontTouch(io)
  childClock := io.clock
  childReset := withClockAndReset(io.clock, io.reset){ ResetGen(dft = Some(io.dft.reset)) }
  def implicitClock = childClock
  def implicitReset = childReset
  io.cio <> outer.cioNode.in.head._1
  io.l2 <> outer.l2Node.in.head._1

  val cpuHalt = Wire(Bool())
  private val pSlv = Module(new PChannelSlv(devActiveBits, PowerMode.powerModeBits))
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
  io.imsic.fromCpu := DontCare
  io.reset_state := withClockAndReset(childClock, childReset) {
    val rstStateReg = RegInit("b111".U(3.W))
    rstStateReg := Cat(0.U(1.W), rstStateReg(2, 1))
    rstStateReg(0)
  }
}
