package linknan

import chisel3._
import chisel3.util._
import freechips.rocketchip.tilelink.{TLBundle, TLBundleParameters}

package object utils {
  def connectByName[T <: Bundle, K <: Bundle](sink: ReadyValidIO[T], src: ReadyValidIO[K]):Unit = {
    sink.valid := src.valid
    src.ready := sink.ready
    sink.bits := DontCare
    val recvMap = sink.bits.elements.map(e => (e._1.toLowerCase, e._2))
    val sendMap = src.bits.elements.map(e => (e._1.toLowerCase, e._2))
    for((name, data) <- recvMap) {
      if(sendMap.contains(name)) data := sendMap(name).asTypeOf(data)
    }
  }

  def connectChiChn(sink: ReadyValidIO[Data], src: ReadyValidIO[Data]):Unit = {
    require(sink.bits.getWidth == src.bits.getWidth)
    sink.valid := src.valid
    sink.bits := src.bits.asTypeOf(sink.bits)
    src.ready := sink.ready
  }

  class BareTLBuffer(params: TLBundleParameters, depth:Int = 2, pipe:Boolean = false) extends Module {
    val io = IO(new Bundle {
      val slv = Flipped(new TLBundle(params))
      val mst = new TLBundle(params)
    })
    io.mst.a <> Queue(io.slv.a, entries = depth, pipe = pipe)
    io.slv.d <> Queue(io.mst.d, entries = depth, pipe = pipe)
    if(params.hasBCE) {
      io.slv.b <> Queue(io.mst.b, entries = depth, pipe = pipe)
      io.mst.c <> Queue(io.slv.c, entries = depth, pipe = pipe)
      io.mst.e <> Queue(io.slv.e, entries = depth, pipe = pipe)
    }
  }
  object BareTlBuffer {
    def apply(mst: TLBundle, depth:Int = 2, pipe:Boolean = false, name:Option[String] = None): TLBundle = {
      val buf = Module(new BareTLBuffer(mst.params, depth = depth, pipe = pipe))
      name.foreach(n => buf.suggestName(n))
      buf.io.slv <> mst
      buf.io.mst
    }
  }
}
