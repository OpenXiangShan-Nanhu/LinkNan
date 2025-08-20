package linknan.soc.device

import chisel3._
import chisel3.util._
import freechips.rocketchip.amba.axi4.{AXI4Buffer, AXI4Deinterleaver, AXI4SlaveNode, AXI4SlaveParameters, AXI4SlavePortParameters, AXI4UserYanker}
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{AddressSet, IdRange, RegionType, TransferSizes}
import freechips.rocketchip.interrupts._
import freechips.rocketchip.resources.BindingScope
import freechips.rocketchip.tile.MaxHartIdBits
import freechips.rocketchip.tilelink.{TLWidthWidget, _}
import freechips.rocketchip.util.AsyncQueueParams
import linknan.soc.LinkNanParamsKey
import linknan.utils.BitSynchronizer
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule._
import xs.utils.dft.BaseTestBundle
import xs.utils.{IntBuffer, ResetGen}
import zhujiang.ZJParametersKey

class TLDeviceBlockIO(coreNum: Int, extIntrNum: Int)(implicit p: Parameters) extends Bundle {
  val extIntr = Input(UInt(extIntrNum.W))
  val meip = Output(UInt(coreNum.W))
  val seip = Output(UInt(coreNum.W))
  val dbip = Output(UInt(coreNum.W))
  val hartAvail = Input(Vec(coreNum, Bool()))
  val resetCtrl = new ResetCtrlIO(coreNum)(p)
  val debug = new DebugIO()(p)
}

class TLDeviceBlockInner(coreNum: Int, extIntrNum: Int)(implicit p: Parameters) extends LazyModule with BindingScope {
  private val tlAsyncSink = LazyModule(new TLAsyncCrossingSink(AsyncQueueParams(1)))
  private val tlAsyncSrc = LazyModule(new TLAsyncCrossingSource())
  val asyncSinkNode = tlAsyncSink.node
  val aysncSourceNode = tlAsyncSrc.node

  private val xbar = LazyModule(new TLXbar)
  private val plic = LazyModule(new TLPLIC(PLICParams(baseAddress = p(LinkNanParamsKey).plicBase), 8))
  private val debug = LazyModule(new DebugModule(coreNum))

  private val intSourceNode = IntSourceNode(IntSourcePortSimple(extIntrNum, ports = 1, sources = 1))
  private val debugIntSink = IntSinkNode(IntSinkPortSimple(coreNum, 1))
  private val plicIntSink = IntSinkNode(IntSinkPortSimple(2 * coreNum, 1))

  xbar.node :=* TLBuffer() :=* asyncSinkNode
  plic.node :*= xbar.node
  debug.debug.node :*= xbar.node
  plic.intnode := IntBuffer(3, cdc = true) := intSourceNode

  debugIntSink :*= IntBuffer(3, cdc = true) :*= debug.debug.dmOuter.dmOuter.intnode
  plicIntSink :*= IntBuffer(3, cdc = true) :*= plic.intnode

  aysncSourceNode :=* TLBuffer() :=* debug.debug.dmInner.dmInner.sb2tlOpt.get.node

  lazy val module = new Impl

  class Impl extends LazyRawModuleImp(this) with ImplicitClock with ImplicitReset {
    override def provideImplicitClockToLazyChildren = true
    val clock = IO(Input(Clock()))
    val reset = IO(Input(Reset()))
    val dfx = IO(new BaseTestBundle)
    private val rstSync = withClockAndReset(clock, reset) { Module(new ResetGen(3)) }
    rstSync.dft := dfx.toResetDftBundle
    val implicitClock = clock
    val implicitReset = rstSync.o_reset
    childClock := implicitClock
    childReset := implicitReset

    val io = IO(new TLDeviceBlockIO(coreNum, extIntrNum))

    require(intSourceNode.out.head._1.length == io.extIntr.getWidth)
    for(idx <- 0 until extIntrNum) {
      intSourceNode.out.head._1(idx) := io.extIntr(idx)
    }
    private val meip = Wire(Vec(coreNum, Bool()))
    private val seip = Wire(Vec(coreNum, Bool()))
    private val dbip = Wire(Vec(coreNum, Bool()))
    io.meip := meip.asUInt
    io.seip := seip.asUInt
    io.dbip := dbip.asUInt
    for(idx <- 0 until coreNum) {
      meip(idx) := plicIntSink.in.map(_._1)(2 * idx).head
      seip(idx) := plicIntSink.in.map(_._1)(2 * idx + 1).head
      dbip(idx) := debugIntSink.in.map(_._1)(idx).head
    }
    debug.module.io.clock := clock.asBool
    debug.module.io.reset := reset
    debug.module.io.resetCtrl <> io.resetCtrl
    debug.module.io.resetCtrl.hartIsInReset.zip(io.resetCtrl.hartIsInReset).foreach({case(a, b) => a := BitSynchronizer(b)})
    debug.module.io.hartAvail.zip(io.hartAvail).foreach({case(a, b) => a := BitSynchronizer(b)})
    debug.module.io.debugIO <> io.debug
    debug.module.io.debugIO.clock := clock
    debug.module.io.debugIO.reset := reset
  }
}

class TLDeviceBlock(coreNum: Int, extIntrNum: Int, cfgIdBits: Int, cfgDataBits: Int, sbaIdBits:Int, sbaDataBits: Int)(implicit p: Parameters) extends LazyModule with BindingScope {
  private val innerP = p.alterPartial({
    case DebugModuleKey => Some(p(LinkNanParamsKey).debugParams)
    case MaxHartIdBits => log2Ceil(coreNum)
    case ExportDebug => DebugAttachParams(protocols = Set(JTAG))
    case JtagDTMKey => JtagDTMKey
  })
  private val clientParameters = TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "riscv-device-block",
      sourceId = IdRange(0, 1 << cfgIdBits),
      supportsProbe = TransferSizes(1, cfgDataBits / 8),
      supportsGet = TransferSizes(1, cfgDataBits / 8),
      supportsPutFull = TransferSizes(1, cfgDataBits / 8),
      supportsPutPartial = TransferSizes(1, cfgDataBits / 8)
    ))
  )
  private val clientNode = TLClientNode(Seq(clientParameters))
  private val sbaNode = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
      address = Seq(AddressSet(0L, (0x1L << p(ZJParametersKey).requestAddrBits) - 1)),
      regionType = RegionType.UNCACHED,
      supportsWrite = TransferSizes(1, sbaDataBits / 8),
      supportsRead = TransferSizes(1, sbaDataBits / 8),
      interleavedId = Some(0)
    )),
    beatBytes = sbaDataBits / 8
  )))
  private val sbaAsyncSink = LazyModule(new TLAsyncCrossingSink(AsyncQueueParams(1)))
  private val cfgAsyncSrc = LazyModule(new TLAsyncCrossingSource())

  private val inner = LazyModule(new TLDeviceBlockInner(coreNum, extIntrNum)(innerP))
  inner.asyncSinkNode :=* cfgAsyncSrc.node :=* clientNode
  sbaNode :*= AXI4Buffer() :*= AXI4UserYanker() :*=
  AXI4Deinterleaver(8) :*= AXI4Buffer() :*=
  TLToAXI4() :*= TLBuffer() :*=
  TLWidthWidget(1) :=*
  sbaAsyncSink.node :=*
  inner.aysncSourceNode

  lazy val module = new Impl
  class Impl extends LazyRawModuleImp(this) with ImplicitClock with ImplicitReset {
    override def provideImplicitClockToLazyChildren = true
    val tlm = clientNode.makeIOs()
    val sba = sbaNode.makeIOs()
    val dfx = IO(new BaseTestBundle)
    val dev = IO(new TLDeviceBlockIO(coreNum, extIntrNum)(innerP))
    val sys_clk = IO(Input(Clock()))
    val dev_clk = IO(Input(Clock()))
    val reset = IO(Input(AsyncReset()))

    val implicitClock = sys_clk
    val implicitReset = reset
    childClock := sys_clk
    childReset := reset

    private val rstSync = Module(new ResetGen(2))
    rstSync.dft := dfx.toResetDftBundle
    rstSync.reset := reset

    inner.module.clock := dev_clk
    inner.module.reset := rstSync.o_reset
    inner.module.dfx := dfx
    inner.module.io <> dev
  }
}