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
  clusterHub.io.icn <> io.icn
  clusterHub.io.core <> io.cpu.chiPdc

  pll.io.cfg := clusterPeriCx.io.cluster.pllCfg
  clusterPeriCx.io.cluster.pllLock := pll.io.lock
  clusterPeriCx.io.cluster.rtc := io.icn.misc.rtc
  pll.io.in_clock := io.icn.osc_clock
  coreCg.io.CK := pll.io.cpu_clock
  coreCg.io.TE := io.icn.dft.func.cgen
  coreCg.io.E := cpuCtl.pcsm.clkEn

  cpuCtl.defaultBootAddr := clusterHub.io.cpu.defaultBootAddr
  if(p(LinkNanParamsKey).removeCore) {
    cpuCtl.defaultEnable := true.B
  } else {
    cpuCtl.defaultEnable := clusterHub.io.cpu.clusterId === 0.U
  }

  clusterHub.io.blockSnp := cpuCtl.blockReq
  private val intrPending = Cat(io.icn.misc.dbip ++ io.icn.misc.meip ++ io.icn.misc.msip ++ io.icn.misc.mtip ++ io.icn.misc.seip).orR
  private val reqToOn = RegNext(intrPending) || RegNext(clusterHub.io.snpPending)

  cpuDev.clock := coreCg.io.Q
  cpuDev.reset := (resetSync.asBool || cpuCtl.pcsm.reset).asAsyncReset
  cpuDev.pchn <> cpuCtl.pchn
  cpuCtl.pchn.active := Cat(reqToOn, false.B, false.B) | RegNext(cpuDev.pchn.active)
  cpuDev.pwrEnReq := cpuCtl.pcsm.pwrReq
  cpuCtl.pcsm.pwrResp := cpuDev.pwrEnAck
  cpuDev.isoEn := cpuCtl.pcsm.isoEn
  cpuDev.mhartid := clusterHub.io.cpu.clusterId
  cpuDev.reset_vector := cpuCtl.bootAddr
  cpuDev.msip := clusterHub.io.cpu.msip(0)
  cpuDev.mtip := clusterHub.io.cpu.mtip(0)
  cpuDev.meip := clusterHub.io.cpu.meip(0)
  cpuDev.seip := clusterHub.io.cpu.seip(0)
  cpuDev.dbip := clusterHub.io.cpu.dbip(0)
  cpuDev.timerUpdate := cpuCtl.timerUpdate
  clusterHub.io.cpu.resetState(0) := RegNext(cpuDev.reset_state, true.B)
  cpuDev.dft := io.icn.dft
  cpuCtl.coreId := cpuDev.mhartid.tail(ciIdBits)
}
