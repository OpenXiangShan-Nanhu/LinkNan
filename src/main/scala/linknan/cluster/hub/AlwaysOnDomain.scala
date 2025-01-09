package linknan.cluster.hub

import chisel3._
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters}
import linknan.cluster.hub.interconnect.{CioXBar, ClusterDeviceBundle, ClusterHub}
import linknan.cluster.power.controller.{PcsmCtrlIO, PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.PChannel
import org.chipsalliance.cde.config.Parameters
import xijiang.{Node, NodeType}
import zhujiang.{DftWires, ZJBundle, ZJParametersKey, ZJRawModule}
import zhujiang.device.bridge.tlul.TLULBridge
import linknan.soc.PeripheralRemapper
import linknan.utils.{BareTlBuffer, connectByName}
import xiangshan.BusErrorUnitInfo
import xijiang.router.base.IcnBundle
import xs.utils.{ClockGate, ClockManagerWrapper, ResetGen}
import zhujiang.chi.ChiBuffer
import zhujiang.device.tlu2chi.TLUL2ChiBridge
import zhujiang.tilelink.TilelinkParams

class CpuDomainCtlBundle(implicit p: Parameters) extends Bundle {
  val pchn = new PChannel(devActiveBits, PowerMode.powerModeBits)
  val pcsm = new PcsmCtrlIO
  val mhartid = Output(UInt(p(ZJParametersKey).clusterIdBits.W))
  val reset_vector = Output(UInt(p(ZJParametersKey).requestAddrBits.W))
  val icacheErr = Input(new BusErrorUnitInfo)
  val dcacheErr = Input(new BusErrorUnitInfo)
  val msip = Output(Bool())
  val mtip = Output(Bool())
  val meip = Output(Bool())
  val seip = Output(Bool())
  val dbip = Output(Bool())
  val imsic = new ImsicBundle
  val reset_state = Input(Bool())
  val blockProbe = Output(Bool())
}

class CsuDomainBundle(node: Node, ioParams:TLBundleParameters)(implicit p: Parameters) extends Bundle {
  val l2cache = new IcnBundle(node)
  val cio = Flipped(new TLBundle(ioParams))
  val dft = Output(new DftWires)
  val pchn = new PChannel(devActiveBits, PowerMode.powerModeBits)
  val pwrEnReq = Output(Bool())
  val pwrEnAck = Input(Bool())
  val isoEn = Output(Bool())
  val clock = Output(Clock())
  val reset = Output(AsyncReset())
}

class AlwaysOnDomainBundle(val node: Node, ioParams:TLBundleParameters)(implicit p: Parameters) extends ZJBundle {
  val csu = new CsuDomainBundle(node, ioParams)
  val cpu = Vec(node.cpuNum, new CpuDomainCtlBundle)
}

class AlwaysOnDomain(node: Node, ioParams:TLBundleParameters)(implicit p: Parameters) extends ZJRawModule
 with ImplicitReset with ImplicitClock {
  val implicitReset = Wire(AsyncReset())
  val implicitClock = Wire(Clock())
  require(node.nodeType == NodeType.CC)
  private val cioParams = TilelinkParams(
    addrBits = ioParams.addressBits,
    sourceBits = ioParams.sourceBits,
    sinkBits = ioParams.sinkBits,
    dataBits = ioParams.dataBits
  )
  private val clusterHub = Module(new ClusterHub(node))
  private val chi2tl = Module(new TLULBridge(clusterHub.io.peripheral.node, 64, 3))
  private val cioXbar = Module(new CioXBar(Seq(cioParams), node.cpuNum))
  private val tl2chi = Module(new TLUL2ChiBridge(clusterHub.io.cio.node, cioXbar.io.downstream.last.params))
  private val clusterPeriCx = Module(new ClusterPeriBlock(Seq(chi2tl.tl.params, cioXbar.io.downstream.head.params), node.cpuNum))

  chi2tl.icn <> clusterHub.io.peripheral
  clusterPeriCx.io.tls.head <> chi2tl.tl
  clusterPeriCx.io.tls.last <> cioXbar.io.downstream.head
  tl2chi.tlm <> cioXbar.io.downstream.last
  clusterHub.io.cio <> tl2chi.icn

  val io = IO(new Bundle {
    val icn = new ClusterDeviceBundle(node)
    val cluster = new AlwaysOnDomainBundle(node.copy(nodeType = NodeType.RF), ioParams)
  })
  private val resetSync = withClockAndReset(implicitClock, io.icn.socket.resetRx) { ResetGen(dft = Some(io.icn.dft.reset)) }
  private val pll = Module(new ClockManagerWrapper)
  private val l2Buf = Module(new ChiBuffer(node.copy(nodeType = NodeType.RF)))
  private val clusterCg = Module(new ClockGate)
  private val cioBuf = BareTlBuffer(io.cluster.csu.cio)
  private val rmp = PeripheralRemapper(cioBuf.a.bits.address, p)
  connectByName(cioXbar.io.upstream.head.a, cioBuf.a)
  connectByName(cioBuf.d, cioXbar.io.upstream.head.d)
  cioXbar.io.upstream.head.a.bits.address := rmp
  io.icn <> clusterHub.io.icn
  l2Buf.io.in <> clusterHub.io.l2cache
  io.cluster.csu.l2cache <> l2Buf.io.out
  cioXbar.misc.chip := clusterHub.io.cpu.mhartid(0)(clusterIdBits - 1, nodeAidBits)

  implicitClock := pll.io.cpu_clock
  implicitReset := resetSync

  pll.io.cfg := clusterPeriCx.io.cluster.pllCfg
  clusterPeriCx.io.cluster.pllLock := pll.io.lock
  pll.io.in_clock := io.icn.osc_clock
  clusterCg.io.CK := pll.io.cpu_clock
  clusterCg.io.E := clusterPeriCx.io.cluster.clusterClkEn
  clusterCg.io.TE := false.B

  io.cluster.csu.clock := clusterCg.io.Q
  io.cluster.csu.reset := (resetSync.asBool | clusterPeriCx.io.csu.pcsm.reset).asAsyncReset
  io.cluster.csu.pchn <> clusterPeriCx.io.csu.pchn
  io.cluster.csu.dft := clusterHub.io.dft
  io.cluster.csu.isoEn := clusterPeriCx.io.csu.pcsm.isoEn
  io.cluster.csu.pwrEnReq := clusterPeriCx.io.csu.pcsm.pwrReq
  clusterPeriCx.io.csu.pcsm.pwrResp := io.cluster.csu.pwrEnAck

  for(i <- 0 until node.cpuNum) {
    val cpuDev = io.cluster.cpu(i)
    val cpuCtl = clusterPeriCx.io.cpu(i)
    cpuCtl.defaultBootAddr := clusterHub.io.cpu.defaultBootAddr(i)
    cpuCtl.defaultEnable := clusterHub.io.cpu.defaultCpuEnable(i)

    cpuDev.pchn <> cpuCtl.pchn
    cpuDev.pcsm <> cpuCtl.pcsm
    cpuDev.mhartid := clusterHub.io.cpu.mhartid(i)
    cpuDev.reset_vector := cpuCtl.bootAddr
    cpuDev.msip := clusterHub.io.cpu.msip(i)
    cpuDev.mtip := clusterHub.io.cpu.mtip(i)
    cpuDev.meip := clusterHub.io.cpu.meip(i)
    cpuDev.seip := clusterHub.io.cpu.seip(i)
    cpuDev.dbip := clusterHub.io.cpu.dbip(i)
    cpuDev.imsic <> cpuCtl.imsic
    cpuDev.blockProbe := cpuCtl.blockProbe
    clusterHub.io.cpu.resetState(i) := cpuDev.reset_state
    cioXbar.misc.core(i) := clusterHub.io.cpu.mhartid(i)(clusterIdBits - nodeAidBits - 1, 0)
    cpuCtl.coreId := clusterHub.io.cpu.mhartid(i)(clusterIdBits - nodeAidBits - 1, 0)
  }
}
