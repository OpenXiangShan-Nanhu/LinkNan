package linknan.cluster.hub.interconnect

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import xijiang.{Node, NodeType}
import xijiang.router.base.IcnBundle
import xs.utils.ResetRRArbiter
import zhujiang.chi._
import zhujiang.device.socket.{ChiPdcIcnSide, IcnPdcBundle, SocketDevSide, SocketDevSideBundle, SocketIcnSideBundle}
import zhujiang.tilelink.TLULBundle
import zhujiang.{DftWires, ZJBundle, ZJModule}

class ClusterAddrBundle(implicit p:Parameters) extends ZJBundle {
  val ci = UInt(ciIdBits.W)
  val tag = UInt((raw - clusterIdBits - zjParams.cpuSpaceBits).W)
  val cpu = UInt(cpuIdBits.W)
  val dev = UInt(zjParams.cpuSpaceBits.W)
  require(this.getWidth == raw)
}

class ClusterMiscWires(node: Node)(implicit p: Parameters) extends ZJBundle {
  val msip = Input(Vec(node.cpuNum, Bool()))
  val mtip = Input(Vec(node.cpuNum, Bool()))
  val meip = Input(Vec(node.cpuNum, Bool()))
  val seip = Input(Vec(node.cpuNum, Bool()))
  val dbip = Input(Vec(node.cpuNum, Bool()))
  val defaultBootAddr = Input(UInt(raw.W))
  val resetState = Output(Vec(node.cpuNum, Bool()))
  val clusterId = Input(UInt(clusterIdBits.W))
  val nodeNid = Input(UInt(nodeNidBits.W))
  val rtcTick = Input(Bool())
}

class ClusterDeviceBundle(node: Node)(implicit p: Parameters) extends ZJBundle {
  val socket = new SocketDevSideBundle(node)
  val misc = new ClusterMiscWires(node)
  val osc_clock = Input(Clock())
  val dft = Input(new DftWires)
  def <> (that: ClusterIcnBundle):Unit = {
    this.socket <> that.socket
    this.misc <> that.misc
    this.osc_clock <> that.osc_clock
    this.dft <> that.dft
  }
}

class ClusterIcnBundle(node: Node)(implicit p: Parameters) extends ZJBundle {
  val socket = new SocketIcnSideBundle(node)
  val misc = Flipped(new ClusterMiscWires(node))
  val osc_clock = Output(Clock())
  val dft = Output(new DftWires)
  def <> (that: ClusterDeviceBundle):Unit = {
    this.socket <> that.socket
    this.misc <> that.misc
    this.osc_clock <> that.osc_clock
    this.dft <> that.dft
  }
}

class ClusterHub(node: Node)(implicit p: Parameters) extends ZJModule {
  require(node.nodeType == NodeType.CC)

  private val socket = Module(new SocketDevSide(node))
  private val pdc = Module(new ChiPdcIcnSide(node.copy(nodeType = NodeType.RF)))
  private val bridge = Module(new ClusterBridge)

  val io = IO(new Bundle {
    val icn = new ClusterDeviceBundle(node)
    val core = new IcnPdcBundle(node.copy(nodeType = NodeType.RF))
    val tlm = new TLULBundle(bridge.io.tlm.params)
    val cpu = Flipped(new ClusterMiscWires(node))
    val pdcxClean = Output(Bool())
    val blockSnp = Input(Bool())
    val snpPending = Output(Bool())
  })
  io.cpu <> io.icn.misc

  socket.io.socket <> io.icn.socket

  io.pdcxClean := pdc.io.clean
  pdc.io.icn <> io.core

  bridge.io.nodeNid := io.icn.misc.nodeNid
  bridge.io.clusterId := io.icn.misc.clusterId
  bridge.io.blockSnp := io.blockSnp
  io.snpPending := bridge.io.snpPending
  bridge.io.icn <> socket.io.icn
  bridge.io.core <> pdc.io.dev
  io.tlm <> bridge.io.tlm
}
