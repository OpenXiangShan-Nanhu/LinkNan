package linknan.soc.device

import chisel3._
import linknan.cluster.hub.peripheral.AclintAddrRemapper
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import zhujiang.axi._
import org.chipsalliance.diplomacy.lazymodule._
import linknan.soc.{LinkNanParamsKey, PeripheralRemapper}
import linknan.utils.connectByName
import org.chipsalliance.cde.config.Parameters
import xijiang.NodeType
import xs.utils.ResetGen
import zhujiang.chi.ReqAddrBundle
import zhujiang.{DftWires, HasZJParams, ZJRawModule}

class ShiftSync[T <: Data](gen:T, sync:Int = 3) extends Module {
  val io = IO(new Bundle{
    val in = Input(gen)
    val out = Output(gen)
  })
  private val syncRegSeq = Seq.fill(sync)(Reg(gen))
  (io.out +: syncRegSeq :+ io.in).reduce((a:T, b:T) => {
    a := b
    a
  })
}

object ShiftSync {
  def apply[T <: Data](in:T, syncStage:Int = 3): T = {
    val sync = Module(new ShiftSync(chiselTypeOf(in)))
    sync.io.in := in
    sync.io.out
  }
}

class AxiCfgXBar(icnAxiParams: AxiParams)(implicit val p: Parameters) extends BaseAxiXbar(Seq(icnAxiParams)) with HasZJParams {
  val misc = IO(new Bundle {
    val ci = Input(UInt(zjParams.ciIdBits.W))
  })
  private def slvMatcher(internal: Boolean)(addr: UInt): Bool = {
    val reqAddr = addr.asTypeOf(new ReqAddrBundle)
    val matchRes = WireInit(reqAddr.ci === misc.ci && 0x0000_0000.U <= reqAddr.devAddr && reqAddr.devAddr < 0x1000_0000.U)
    if(internal) {
      matchRes
    } else {
      !matchRes
    }
  }
  val slvMatchersSeq = Seq(slvMatcher(internal = true), slvMatcher(internal = false))
  initialize()
}

class DevicesWrapper(cfgParams: AxiParams, dmaParams: AxiParams)(implicit p: Parameters) extends ZJRawModule
  with ImplicitClock with ImplicitReset {
  val clock = IO(Input(Clock()))
  val reset = IO(Input(AsyncReset()))
  val implicitClock = clock
  val implicitReset = Wire(AsyncReset())

  private val coreNum = zjParams.island.filter(_.nodeType == NodeType.CC).map(_.cpuNum).sum
  private val extIntrNum = p(LinkNanParamsKey).nrExtIntr
  private val extDmaParams = dmaParams.copy(idBits = dmaParams.idBits - 1)

  private val cfgXBar = Module(new AxiCfgXBar(cfgParams))
  private val cfgBuf = Module(new AxiBuffer(cfgXBar.io.downstream.last.params))
  private val axi2tl = Module(new AxiLite2TLUL(cfgXBar.io.downstream.head.params))
  private val tl2axi = Module(new TLUL2AxiLite(dmaParams))

  private val tlDevBlock = LazyModule(new TLDeviceBlock(
    coreNum,
    extIntrNum,
    axi2tl.io.tl.params.sourceBits,
    axi2tl.io.tl.params.dataBits,
    extDmaParams.dataBits
  )(p.alterPartial {
    case MonitorsEnabled => false
  }))
  private val pb = Module(tlDevBlock.module)

  val io = IO(new Bundle {
    val slv = Flipped(new AxiBundle(cfgParams))
    val mst = new AxiBundle(dmaParams)

    val ext = new Bundle {
      val cfg = new AxiBundle(cfgXBar.io.downstream.last.params.copy(attr = cfgParams.attr))
      val intr = Input(UInt(extIntrNum.W))
    }

    val cpu = new Bundle {
      val meip = Output(UInt(coreNum.W))
      val seip = Output(UInt(coreNum.W))
      val dbip = Output(UInt(coreNum.W))
    }
    val ci = Input(UInt(ciIdBits.W))
    val debug = pb.dev.debug.cloneType
    val resetCtrl = pb.dev.resetCtrl.cloneType
    val dft = Input(new DftWires)
  })
  private val resetGen = Module(new ResetGen)
  resetGen.clock := clock
  resetGen.dft := io.dft.reset
  implicitReset := resetGen.o_reset
  resetGen.reset := reset
  dontTouch(io)

  cfgXBar.misc.ci := io.ci
  cfgXBar.io.upstream.head <> AxiBuffer(io.slv, name = Some("slv_port_buf"))
  axi2tl.io.axi <> cfgXBar.io.downstream.head
  cfgBuf.io.in <> cfgXBar.io.downstream.last
  io.ext.cfg <> cfgBuf.io.out

  private val mstAxi = AxiBuffer(tl2axi.io.axi, name = Some("mst_port_buf"))
  io.mst <> mstAxi
  io.mst.aw.bits.addr := AclintAddrRemapper(mstAxi.aw.bits.addr)
  io.mst.ar.bits.addr := AclintAddrRemapper(mstAxi.ar.bits.addr)

  pb.tlm.foreach(tlm => {
    connectByName(tlm.a, axi2tl.io.tl.a)
    connectByName(axi2tl.io.tl.d, tlm.d)
  })

  pb.sba.foreach(sba => {
    connectByName(tl2axi.io.tl.a, sba.a)
    connectByName(sba.d, tl2axi.io.tl.d)
  })
  pb.dfx := io.dft
  io.cpu.meip := ShiftSync(pb.dev.meip)
  io.cpu.seip := ShiftSync(pb.dev.seip)
  io.cpu.dbip := ShiftSync(pb.dev.dbip)
  pb.dev.extIntr := io.ext.intr
  pb.dev.debug <> io.debug
  io.resetCtrl.hartResetReq.foreach(_ := ShiftSync(pb.dev.resetCtrl.hartResetReq.get))
  pb.dev.resetCtrl.hartIsInReset := ShiftSync(io.resetCtrl.hartIsInReset)
}