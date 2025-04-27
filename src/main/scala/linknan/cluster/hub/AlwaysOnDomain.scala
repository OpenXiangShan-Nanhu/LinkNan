package linknan.cluster.hub

import chisel3._
import chisel3.util.{Cat, Pipe, Valid}
import linknan.cluster.core.CoreWrapperIO
import linknan.cluster.hub.interconnect.{ClusterDeviceBundle, ClusterHub}
import org.chipsalliance.cde.config.Parameters
import xijiang.{Node, NodeType}
import zhujiang.ZJRawModule
import linknan.soc.LinkNanParamsKey
import xs.utils.{ClockGate, ClockManagerWrapper, ResetGen}

class AlwaysOnDomain(node: Node)(implicit p: Parameters) extends ZJRawModule
 with ImplicitReset with ImplicitClock {
  val implicitReset = Wire(AsyncReset())
  val implicitClock = Wire(Clock())
  require(node.nodeType == NodeType.CC)
  require(node.cpuNum == 1)
  private val clusterHub = Module(new ClusterHub(node))
  private val clusterPeriCx = Module(new ClusterPeriBlock(Seq(clusterHub.io.tlm.params), node.cpuNum))

  val io = IO(new Bundle {
    val icn = new ClusterDeviceBundle(node)
    val cpu = Flipped(new CoreWrapperIO(node.copy(nodeType = NodeType.RF)))
  })
  private val resetSync = withClockAndReset(implicitClock, io.icn.socket.resetRx) { ResetGen(dft = Some(io.icn.dft.reset)) }
  private val pll = Module(new ClockManagerWrapper)
  private val coreCg = Module(new ClockGate)
  private val cpuCtl = clusterPeriCx.io.cpu.head
  private val cpuDev = io.cpu

  implicitClock := pll.io.cpu_clock
  implicitReset := resetSync

  clusterPeriCx.io.tls.head <> clusterHub.io.tlm
  clusterHub.io.socket <> io.icn.socket
  clusterHub.io.core <> io.cpu.chiPdc
  clusterHub.io.nodeNid := io.icn.misc.nodeNid
  clusterHub.io.clusterId := io.icn.misc.clusterId

  pll.io.cfg := clusterPeriCx.io.cluster.pllCfg
  clusterPeriCx.io.cluster.pllLock := pll.io.lock
  clusterPeriCx.io.cluster.rtc := io.icn.misc.rtc
  pll.io.in_clock := io.icn.osc_clock
  coreCg.io.CK := pll.io.cpu_clock
  coreCg.io.TE := io.icn.dft.func.cgen
  coreCg.io.E := cpuCtl.pcsm.clkEn

  cpuCtl.defaultBootAddr := io.icn.misc.defaultBootAddr
  if(p(LinkNanParamsKey).removeCore) {
    cpuCtl.defaultEnable := true.B
  } else {
    cpuCtl.defaultEnable := io.icn.misc.clusterId === 0.U
  }

  clusterHub.io.blockSnp := cpuCtl.blockReq
  private val intrPending = Cat(cpuDev.msip, cpuDev.mtip, cpuDev.meip, cpuDev.seip, cpuDev.dbip).orR
  private val reqToOn = RegNext(intrPending) || RegNext(clusterHub.io.snpPending)

  cpuDev.clock := coreCg.io.Q
  cpuDev.reset := (resetSync.asBool || cpuCtl.pcsm.reset).asAsyncReset
  cpuDev.pchn <> cpuCtl.pchn
  cpuCtl.pchn.active := Cat(reqToOn, false.B, false.B) | RegNext(cpuDev.pchn.active)
  cpuDev.pwrEnReq := cpuCtl.pcsm.pwrReq
  cpuCtl.pcsm.pwrResp := cpuDev.pwrEnAck
  cpuDev.isoEn := cpuCtl.pcsm.isoEn
  cpuDev.mhartid := io.icn.misc.clusterId
  cpuDev.reset_vector := cpuCtl.bootAddr
  cpuDev.msip := cpuCtl.msip
  cpuDev.mtip := cpuCtl.mtip
  cpuDev.meip := RegNext(io.icn.misc.meip(0))
  cpuDev.seip := RegNext(io.icn.misc.seip(0))
  cpuDev.dbip := RegNext(io.icn.misc.dbip(0))
  cpuDev.timerUpdate := cpuCtl.timerUpdate
  io.icn.misc.resetState(0) := withReset(cpuDev.reset) { RegNext(cpuDev.reset_state, true.B) }
  cpuDev.dft := io.icn.dft
  cpuDev.ramctl := io.icn.ramctl
  cpuCtl.coreId := cpuDev.mhartid.tail(ciIdBits)
}
