package linknan.cluster.hub.interconnect

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import xijiang.{Node, NodeType}
import xijiang.router.base.IcnBundle
import xs.utils.dft.{BaseTestBundle, PowerDomainTestBundle}
import xs.utils.sram.SramCtrlBundle
import zhujiang.device.socket.{SocketDevSide, SocketDevSideBundle, SocketIcnSideBundle}
import zhujiang.tilelink.TLULBundle
import zhujiang.{ZJBundle, ZJModule}

class CpuClusterDftWires extends BaseTestBundle {
  val core = new PowerDomainTestBundle(true)
}

class ClusterAddrBundle(implicit p:Parameters) extends ZJBundle {
  val ci = UInt(ciIdBits.W)
  val tag = UInt((raw - clusterIdBits - zjParams.cpuSpaceBits).W)
  val cpu = UInt(cpuIdBits.W)
  val dev = UInt(zjParams.cpuSpaceBits.W)
  require(this.getWidth == raw)
}

class ClusterMiscWires(node: Node)(implicit p: Parameters) extends ZJBundle {
  val meip = Input(Vec(node.cpuNum, Bool()))
  val seip = Input(Vec(node.cpuNum, Bool()))
  val dbip = Input(Vec(node.cpuNum, Bool()))
  val defaultBootAddr = Input(UInt(raw.W))
  val resetState = Output(Vec(node.cpuNum, Bool()))
  val avail = Output(Vec(node.cpuNum, Bool()))
  val clusterId = Input(UInt(clusterIdBits.W))
  val nodeNid = Input(UInt(nodeNidBits.W))
  val rtc = Input(Bool())
}

class ClusterDeviceBundle(node: Node)(implicit p: Parameters) extends ZJBundle {
  val socket = new SocketDevSideBundle(node)
  val misc = new ClusterMiscWires(node)
  val cpu_clock = Input(Clock())
  val noc_clock = Input(Clock())
  val dft = new CpuClusterDftWires
  val ramctl = Input(new SramCtrlBundle)
  def <> (that: ClusterIcnBundle):Unit = {
    this.socket <> that.socket
    this.misc <> that.misc
    this.cpu_clock <> that.cpu_clock
    this.noc_clock <> that.noc_clock
    this.dft <> that.dft
    this.ramctl <> that.ramctl
  }
}

class ClusterIcnBundle(node: Node)(implicit p: Parameters) extends ZJBundle {
  val socket = new SocketIcnSideBundle(node)
  val misc = Flipped(new ClusterMiscWires(node))
  val cpu_clock = Output(Clock())
  val noc_clock = Output(Clock())
  val dft = Flipped(new CpuClusterDftWires)
  val ramctl = Output(new SramCtrlBundle)
  def <> (that: ClusterDeviceBundle):Unit = {
    this.socket <> that.socket
    this.misc <> that.misc
    this.cpu_clock <> that.cpu_clock
    this.noc_clock <> that.noc_clock
    this.dft <> that.dft
    this.ramctl <> that.ramctl
  }
}

class ClusterHub(node: Node)(implicit p: Parameters) extends ZJModule {
  require(node.nodeType == NodeType.CC)

  private val socket = Module(new SocketDevSide(node))
  private val bridge = Module(new ClusterBridge(node))

  val io = IO(new Bundle {
    val socket = new SocketDevSideBundle(node)
    val core = new IcnBundle(node.copy(nodeType = NodeType.RF))
    val tlm = new TLULBundle(bridge.io.tlm.params)
    val nodeNid = Input(UInt(nodeNidBits.W))
    val clusterId = Input(UInt(clusterIdBits.W))
    val blockSnp = Input(Bool())
    val snpPending = Output(Bool())
  })
  socket.io.socket <> io.socket

  bridge.io.nodeNid := io.nodeNid
  bridge.io.clusterId := io.clusterId
  bridge.io.icn <> socket.io.icn
  bridge.io.core <> io.core
  io.tlm <> bridge.io.tlm

  io.core.tx.snoop.get.valid := bridge.io.core.tx.snoop.get.valid & !io.blockSnp
  bridge.io.core.tx.snoop.get.ready := io.core.tx.snoop.get.ready & !io.blockSnp
  io.snpPending := RegNext(io.blockSnp & bridge.io.core.tx.snoop.get.valid)
}
