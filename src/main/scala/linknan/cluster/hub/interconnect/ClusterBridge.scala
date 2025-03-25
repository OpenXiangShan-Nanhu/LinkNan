package linknan.cluster.hub.interconnect

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import xijiang.router.base.{DeviceIcnBundle, IcnBundle}
import xijiang.{Node, NodeType}
import xs.utils.ResetRRArbiter
import xs.utils.queue.FastQueue
import zhujiang.{HasZJParams, ZJModule}
import zhujiang.chi.{DataFlit, Flit, NodeIdBundle, RReqFlit, ReqAddrBundle, RespFlit, RingFlit}
import zhujiang.device.bridge.tlul.TLULBridge
import zhujiang.tilelink.TLULBundle

trait ClusterInterconnectHelper {
  m: ZJModule =>
  private def getNodeId(routerId:UInt, aid:Int):UInt = {
    val res = Wire(new NodeIdBundle)
    res.nid := routerId
    res.aid := aid.U
    res.asUInt
  }
  def getPeriNodeId(routerId:UInt):UInt = getNodeId(routerId, 0)
  def getCoreNodeId(routerId:UInt):UInt = getNodeId(routerId, 1)

  def conn[T <: Data](slvs: Seq[(DecoupledIO[T], T => Bool, Seq[Int])], msts: Seq[DecoupledIO[T]], name:String): Unit = {
    val downstream = slvs.map(_._1)
    val slvMatchersSeq = slvs.map(_._2)
    val slvDrivers = slvs.map(_._3)
    val upstream = msts
    val slvSize = slvs.size
    val mstSize = msts.size
    val dnStrmRdyMat = Wire(Vec(mstSize, Vec(slvSize, Bool())))
    dontTouch(dnStrmRdyMat)
    dnStrmRdyMat.suggestName(s"${name}_dn_strm_rdy_mat")
    for(sidx <- slvs.indices) {
      val dirvers = slvDrivers(sidx)
      val arb = Module(new ResetRRArbiter(downstream.head.bits.cloneType, dirvers.size))
      val q = Module(new Queue(downstream.head.bits.cloneType, entries = 2))
      arb.suggestName(s"${name}_arb_$sidx")
      q.suggestName(s"${name}_q_$sidx")
      q.io.enq <> arb.io.out
      downstream(sidx) <> q.io.deq

      for(midx <- 0 until mstSize) {
        val uin = upstream(midx)
        if(dirvers.contains(midx)) {
          val ain = arb.io.in(dirvers.indexOf(midx))
          val correctSlv = slvMatchersSeq(sidx)(uin.bits)
          ain.valid := uin.valid && correctSlv
          ain.bits := uin.bits
          dnStrmRdyMat(midx)(sidx) := ain.ready && correctSlv
        } else {
          dnStrmRdyMat(midx)(sidx) := false.B
        }
        when(uin.valid) {
          assert(PopCount(dnStrmRdyMat(midx)) <= 1.U)
        }
      }
    }
    for(midx <- 0 until mstSize) {
      upstream(midx).ready := upstream(midx).valid && Cat(dnStrmRdyMat(midx)).orR
    }
  }
}

class ReqXBar(implicit p: Parameters) extends ZJModule with ClusterInterconnectHelper {
  val io = IO(new Bundle {
    val clusterId = Input(UInt(clusterIdBits.W))
    val rx = new Bundle {
      val icn = Flipped(Decoupled(new RReqFlit))
      val core = Flipped(Decoupled(new RReqFlit))
    }
    val tx = new Bundle {
      val icn = Decoupled(new RReqFlit)
      val peri = Decoupled(new RReqFlit)
    }
  })
  private val ci = io.clusterId.head(ciIdBits)
  private val hart = io.clusterId.tail(ciIdBits)
  private val cpb = zjParams.cpuSpaceBits
  private val periMatch = (flit:RReqFlit) => {
    val addr = flit.Addr
    addr.head(ciIdBits) === ci && addr(cpuIdBits + cpb - 1, cpb) === hart
  }
  private val icnMatch = (flit:RReqFlit) => !periMatch(flit)

  private val slvs = Seq(
    (io.tx.icn, icnMatch, Seq(0, 1)),
    (io.tx.peri, periMatch, Seq(0, 1))
  )
  private val msts = Seq(io.rx.icn, io.rx.core)
  conn(slvs, msts, "req")
}

class RspXBar(implicit p: Parameters) extends ZJModule with ClusterInterconnectHelper {
  private val flit = new RingFlit(respFlitBits)
  val io = IO(new Bundle {
    val nodeNid = Input(UInt(nodeNidBits.W))
    val rx = new Bundle {
      val icn = Flipped(Decoupled(flit))
      val core = Flipped(Decoupled(flit))
      val peri = Flipped(Decoupled(flit))
    }
    val tx = new Bundle {
      val icn = Decoupled(flit)
      val core = Decoupled(flit)
      val peri = Decoupled(flit)
    }
  })
  private val periNodeId = getPeriNodeId(io.nodeNid)
  private val coreNodeId = getCoreNodeId(io.nodeNid)

  private val periMatcher = (flit:RingFlit) => flit.TgtID === periNodeId
  private val coreMatcher = (flit:RingFlit) => flit.TgtID === coreNodeId
  private val icnMatcher = (flit:RingFlit) => !periMatcher(flit) && !coreMatcher(flit)

  private val slvs = Seq(
    (io.tx.icn, icnMatcher, Seq(1 ,2)),
    (io.tx.core, coreMatcher, Seq(0 ,2)),
    (io.tx.peri, periMatcher, Seq(0 ,1)),
  )
  private val msts = Seq(io.rx.icn, io.rx.core, io.rx.peri)
  conn(slvs, msts, "rsp")
}

class DatXBar(implicit p: Parameters) extends ZJModule with ClusterInterconnectHelper {
  private val flit = new RingFlit(dataFlitBits)
  val io = IO(new Bundle {
    val nodeNid = Input(UInt(nodeNidBits.W))
    val rx = new Bundle {
      val icn = Flipped(Decoupled(flit))
      val core = Flipped(Decoupled(flit))
      val peri = Flipped(Decoupled(flit))
    }
    val tx = new Bundle {
      val icn = Decoupled(flit)
      val core = Decoupled(flit)
      val peri = Decoupled(flit)
    }
  })
  private val periNodeId = getPeriNodeId(io.nodeNid)
  private val coreNodeId = getCoreNodeId(io.nodeNid)

  private val periMatcher = (flit:RingFlit) => flit.TgtID === periNodeId
  private val coreMatcher = (flit:RingFlit) => flit.TgtID === coreNodeId
  private val icnMatcher = (flit:RingFlit) => !periMatcher(flit) && !coreMatcher(flit)

  private val slvs = Seq(
    (io.tx.icn, icnMatcher, Seq(1 ,2)),
    (io.tx.core, coreMatcher, Seq(0 ,2)),
    (io.tx.peri, periMatcher, Seq(0 ,1)),
  )
  private val msts = Seq(io.rx.icn, io.rx.core, io.rx.peri)
  conn(slvs, msts, "dat")
}

class ClusterBridge(implicit p: Parameters) extends ZJModule with ClusterInterconnectHelper {
  private val chi2tl = Module(new TLULBridge(Node(nodeType = NodeType.HI, splitFlit = true), 64, 3))
  val io = IO(new Bundle {
    val nodeNid = Input(UInt(nodeNidBits.W))
    val clusterId = Input(UInt(clusterIdBits.W))
    val icn = new DeviceIcnBundle(Node(nodeType = NodeType.CC))
    val core = new IcnBundle(Node(nodeType = NodeType.RF))
    val tlm = new TLULBundle(chi2tl.tl.params)
    val blockSnp = Input(Bool())
    val snpPending = Output(Bool())
  })

  chi2tl.icn.tx.req.get.ready := false.B
  io.tlm <> chi2tl.tl

  private def connChn(rx:DecoupledIO[Data], tx:DecoupledIO[Data], nodeId:Option[UInt]):Unit = {
    require(tx.bits.getWidth == rx.bits.getWidth)
    val tmp = Wire(new RingFlit(rx.bits.getWidth))
    tmp := tx.bits.asTypeOf(tmp)
    nodeId.foreach(n => tmp.SrcID := n)
    rx.valid := tx.valid
    rx.bits := tmp.asTypeOf(rx.bits)
    tx.ready := rx.ready
  }
  private val periNodeId = getPeriNodeId(io.nodeNid)
  private val coreNodeId = getCoreNodeId(io.nodeNid)

  private val reqXbar = Module(new ReqXBar)
  reqXbar.io.clusterId := io.clusterId
  connChn(reqXbar.io.rx.icn, io.icn.rx.req.get, None)
  connChn(reqXbar.io.rx.core, io.core.rx.req.get, Some(coreNodeId))
  connChn(io.icn.tx.req.get, reqXbar.io.tx.icn, None)
  connChn(chi2tl.icn.rx.req.get, reqXbar.io.tx.peri, None)

  private val rspXbar = Module(new RspXBar)
  rspXbar.io.nodeNid := io.nodeNid
  connChn(rspXbar.io.rx.icn, io.icn.rx.resp.get, None)
  connChn(rspXbar.io.rx.core, io.core.rx.resp.get, Some(coreNodeId))
  connChn(rspXbar.io.rx.peri, chi2tl.icn.tx.resp.get, Some(periNodeId))
  connChn(io.icn.tx.resp.get, rspXbar.io.tx.icn, None)
  connChn(io.core.tx.resp.get, rspXbar.io.tx.core, None)
  connChn(chi2tl.icn.rx.resp.get, rspXbar.io.tx.peri, None)

  private val datXbar = Module(new DatXBar)
  datXbar.io.nodeNid := io.nodeNid
  connChn(datXbar.io.rx.icn, io.icn.rx.data.get, None)
  connChn(datXbar.io.rx.core, io.core.rx.data.get, Some(coreNodeId))
  connChn(datXbar.io.rx.peri, chi2tl.icn.tx.data.get, Some(periNodeId))
  connChn(io.icn.tx.data.get, datXbar.io.tx.icn, None)
  connChn(io.core.tx.data.get, datXbar.io.tx.core, None)
  connChn(chi2tl.icn.rx.data.get, datXbar.io.tx.peri, None)

  connChn(io.core.tx.snoop.get, io.icn.rx.snoop.get, None)
  io.core.tx.snoop.get.valid := io.icn.rx.snoop.get.valid & !io.blockSnp
  io.icn.rx.snoop.get.ready := io.core.tx.snoop.get.ready & !io.blockSnp
  io.snpPending := RegNext(io.blockSnp & io.icn.rx.snoop.get.valid)
}
