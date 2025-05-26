package linknan.soc.device

import aia.TLAPLIC
import chisel3._
import chisel3.util._
import freechips.rocketchip.devices.debug._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.diplomacy.{AddressSet, IdRange, TransferSizes}
import freechips.rocketchip.resources.BindingScope
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.tile.MaxHartIdBits
import freechips.rocketchip.tilelink._
import freechips.rocketchip.util.{FastToSlow, SlowToFast}
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xs.utils.{ClockGate, DFTResetSignals, IntBuffer, ResetGen}
import zhujiang.{DftWires, ZJParametersKey}

class TLDeviceBlockIO(coreNum: Int, extIntrNum: Int)(implicit p: Parameters) extends Bundle {
  val extIntr = Input(UInt(extIntrNum.W))
  val meip = Output(UInt(coreNum.W))
  val seip = Output(UInt(coreNum.W))
  val dbip = Output(UInt(coreNum.W))
  val resetCtrl = new ResetCtrlIO(coreNum)(p)
  val debug = new DebugIO()(p)
}

class TLDeviceBlockInner(coreNum: Int, extIntrNum: Int)(implicit p: Parameters) extends LazyModule with BindingScope {
  val rationalSinkNode = TLRationalCrossingSink(FastToSlow)
  val rationalSourceNode = TLRationalCrossingSource()

  private val xbar = LazyModule(new TLXbar)
  private val plic = LazyModule(new TLPLIC(PLICParams(baseAddress = p(LinkNanParamsKey).plicBase), 8))
  private val debug = LazyModule(new DebugModule(coreNum))
  private val sbaXBar = LazyModule(new TLXbar)

  private val intSourceNode = IntSourceNode(IntSourcePortSimple(extIntrNum, ports = 1, sources = 1))
  private val debugIntSink = IntSinkNode(IntSinkPortSimple(coreNum, 1))
  private val plicIntSink = IntSinkNode(IntSinkPortSimple(2 * coreNum, 1))

  xbar.node :=* TLBuffer() :=* rationalSinkNode
  plic.node :*= xbar.node
  debug.debug.node :*= xbar.node
  plic.intnode := IntBuffer(3, cdc = true) := intSourceNode

  debugIntSink :*= IntBuffer(3, cdc = true) :*= debug.debug.dmOuter.dmOuter.intnode
  plicIntSink :*= IntBuffer(3, cdc = true) :*= plic.intnode

  sbaXBar.node :=* TLBuffer() :=* TLWidthWidget(1) :=* debug.debug.dmInner.dmInner.sb2tlOpt.get.node
  rationalSourceNode :=* TLBuffer() :=* sbaXBar.node

  lazy val module = new Impl

  class Impl extends LazyRawModuleImp(this) with ImplicitClock with ImplicitReset {
    override def provideImplicitClockToLazyChildren = true
    val clock = IO(Input(Clock()))
    val reset = IO(Input(Reset()))
    val dfx = IO(Input(new DftWires))
    private val rstSync = withClockAndReset(clock, reset) { Module(new ResetGen(3)) }
    rstSync.dft := dfx.reset
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
    debug.module.io.debugIO <> io.debug
    debug.module.io.debugIO.clock := clock
    debug.module.io.debugIO.reset := reset
  }
}

class TLDeviceBlock(coreNum: Int, extIntrNum: Int, idBits: Int, cfgDataBits: Int, sbaDataBits: Int)(implicit p: Parameters) extends LazyModule with BindingScope {
  private val innerP = p.alterPartial({
    case DebugModuleKey => Some(p(LinkNanParamsKey).debugParams)
    case MaxHartIdBits => log2Ceil(coreNum)
    case ExportDebug => DebugAttachParams(protocols = Set(JTAG))
    case JtagDTMKey => JtagDTMKey
  })
  private val clientParameters = TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "riscv-device-block",
      sourceId = IdRange(0, 1 << idBits),
      supportsProbe = TransferSizes(1, cfgDataBits / 8),
      supportsGet = TransferSizes(1, cfgDataBits / 8),
      supportsPutFull = TransferSizes(1, cfgDataBits / 8),
      supportsPutPartial = TransferSizes(1, cfgDataBits / 8)
    ))
  )
  private val clientNode = TLClientNode(Seq(clientParameters))
  private val sbaParameters = TLSlavePortParameters.v1(
    managers = Seq(TLSlaveParameters.v1(
      address = Seq(AddressSet(0L, (0x1L << p(ZJParametersKey).requestAddrBits) - 1)),
      supportsGet = TransferSizes(1, sbaDataBits / 8),
      supportsPutFull = TransferSizes(1, sbaDataBits / 8),
      supportsPutPartial = TransferSizes(1, sbaDataBits / 8),
    )),
    beatBytes = sbaDataBits / 8
  )
  private val sbaNode = TLManagerNode(Seq(sbaParameters))

  private val inner = LazyModule(new TLDeviceBlockInner(coreNum, extIntrNum)(innerP))
  inner.rationalSinkNode :=* TLRationalCrossingSource() :=* clientNode
  sbaNode :=* TLRationalCrossingSink(SlowToFast) :=* inner.rationalSourceNode

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val tlm = clientNode.makeIOs()
    val sba = sbaNode.makeIOs()
    val dfx = IO(Input(new DftWires))
    val dev = IO(new TLDeviceBlockIO(coreNum, extIntrNum)(innerP))

    private val cg = Module(new ClockGate)
    private val ff = Module(new ClkDiv2Reg)
    private val rstSync = Module(new ResetGen(2))
    ff.io.clock := clock
    cg.io.E := ff.io.out
    cg.io.CK := clock
    cg.io.TE := false.B
    rstSync.dft := dfx.reset
    rstSync.reset := reset

    inner.module.clock := cg.io.Q
    inner.module.reset := rstSync.o_reset
    inner.module.dfx := dfx
    inner.module.io <> dev
  }
}

class ClkDiv2Reg extends BlackBox with HasBlackBoxInline {
  override val desiredName = xs.utils.GlobalData.prefix + "ClkDiv2Reg"
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val out = Bool()
  })
  setInline(s"$desiredName.sv",
    s"""
       |module $desiredName (
       |  input wire clock,
       |  output reg out
       |);
       |`ifndef SYNTHESIS
       |  initial out = 1'b1;
       |`endif
       |
       |always @ (posedge clock) out <= ~out;
       |endmodule""".stripMargin)
}