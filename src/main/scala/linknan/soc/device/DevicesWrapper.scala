package linknan.soc.device

import chisel3._
import chisel3.util.Cat
import linknan.cluster.hub.peripheral.AclintAddrRemapper
import linknan.generator.AddrConfig
import linknan.soc.LinkNanParamsKey
import linknan.utils.connectByName
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule._
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import xijiang.NodeType
import xs.utils.ResetGen
import xs.utils.dft.BaseTestBundle
import zhujiang.axi._
import zhujiang.{HasZJParams, ZJRawModule}

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
  private val idas = p(LinkNanParamsKey).internalDeviceAdressSets
  private def slvMatcher(internal: Boolean)(addr: UInt): Bool = {
    val addrMatchVec = idas.map(as => {
      val base = as.base.U(raw.W)
      val mask = (~(as.mask.U(raw.W))).asUInt
      (addr & mask) === base
    })
    val matchRes = WireInit(Cat(addrMatchVec).orR)
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
  val sys_clk = IO(Input(Clock()))
  val dev_clk = IO(Input(Clock()))
  val reset = IO(Input(AsyncReset()))
  val implicitClock = sys_clk
  val implicitReset = Wire(AsyncReset())

  private val coreNum = zjParams.island.filter(_.nodeType == NodeType.CC).map(_.cpuNum).sum
  private val extIntrNum = p(LinkNanParamsKey).nrExtIntr
  private val extDmaParams = dmaParams

  private val cfgXBar = Module(new AxiCfgXBar(cfgParams))
  private val axi2tl = Module(new AxiLite2TLUL(cfgXBar.io.downstream.head.params))

  private val tlDevBlock = LazyModule(new TLDeviceBlock(
    coreNum,
    extIntrNum,
    axi2tl.io.tl.params.sourceBits,
    axi2tl.io.tl.params.dataBits,
    extDmaParams.idBits,
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
      val hartAvail = Input(Vec(coreNum, Bool()))
    }
    val ci = Input(UInt(ciIdBits.W))
    val debug = pb.dev.debug.cloneType
    val resetCtrl = pb.dev.resetCtrl.cloneType
    val dft = new BaseTestBundle
  })
  private val resetGen = Module(new ResetGen)
  resetGen.clock := sys_clk
  resetGen.dft := io.dft.toResetDftBundle
  implicitReset := resetGen.o_reset
  resetGen.reset := reset
  dontTouch(io)

  cfgXBar.misc.ci := io.ci
  cfgXBar.io.upstream.head <> AxiBuffer(io.slv, name = Some("slv_port_buf"))
  axi2tl.io.axi <> cfgXBar.io.downstream.head
  io.ext.cfg <> AxiBuffer(cfgXBar.io.downstream.last)

  pb.sys_clk := sys_clk
  pb.dev_clk := dev_clk
  pb.reset := implicitReset
  pb.tlm.foreach(tlm => {
    connectByName(tlm.a, axi2tl.io.tl.a)
    connectByName(axi2tl.io.tl.d, tlm.d)
  })

  pb.sba.foreach(sba => {
    connectByName(io.mst.aw, sba.aw)
    connectByName(io.mst.ar, sba.ar)
    connectByName(io.mst.w, sba.w)
    connectByName(sba.b, io.mst.b)
    connectByName(sba.r, io.mst.r)
    io.mst.aw.bits.addr := AclintAddrRemapper(sba.aw.bits.addr)
    io.mst.ar.bits.addr := AclintAddrRemapper(sba.ar.bits.addr)
    io.mst.aw.bits.cache := Mux(sba.aw.bits.addr < AddrConfig.pmemRange.lower.U(raw.W), "b0000".U, "b1010".U)
    io.mst.ar.bits.cache := Mux(sba.ar.bits.addr < AddrConfig.pmemRange.lower.U(raw.W), "b0000".U, "b1010".U)
  })
  pb.dfx := io.dft
  io.cpu.meip := pb.dev.meip
  io.cpu.seip := pb.dev.seip
  io.cpu.dbip := pb.dev.dbip
  pb.dev.extIntr := io.ext.intr
  pb.dev.debug <> io.debug
  pb.dev.hartAvail := io.cpu.hartAvail
  io.resetCtrl.hartResetReq.foreach(_ := pb.dev.resetCtrl.hartResetReq.get)
  pb.dev.resetCtrl.hartIsInReset := io.resetCtrl.hartIsInReset
}