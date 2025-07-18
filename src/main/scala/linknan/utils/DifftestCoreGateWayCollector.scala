package linknan.utils

import chisel3._
import chisel3.util._
import difftest.gateway.{CoreGateway, CoreGatewayBundle}

import scala.collection.SeqMap
import scala.collection.mutable

class DifftestCoreGateWayCollector(ccid:Int, path:String) extends BlackBox with HasBlackBoxInline {
  override def desiredName = xs.utils.GlobalData.prefix + "DifftestCoreGateWayCollector"
  val io = IO(new Bundle {
    val probe = Output(new CoreGatewayBundle)
    val reset = Output(Bool())
    val clock = Output(Clock())
  })

  private val cc = s"$path.cc_$ccid"
  private val portQueue = mutable.Queue[String]()
  private def opStr(name:String, data:Data):String = {
    val width = data.getWidth
    val range = if(width == 1) "         " else f" [${width - 1}%2d: 0] "
    s"  output$range$name"
  }
  private def parseIO(name:String, data:Data): Unit = {
    data match {
      case b: Bundle => b.elements.foreach({case(n, d) => parseIO(s"${name}_$n", d)})
      case v: Vec[Data] => v.zipWithIndex.foreach({case(d, i) => parseIO(s"${name}_$i", d)})
      case _ => if(data.getWidth != 0) portQueue.addOne(opStr(name, data))
    }
  }
  io.probe.elements.foreach({case(n, d) => parseIO(s"probe_$n", d)})
  private val portDecl = portQueue.mkString(",\n")

  private val xmrQueue = mutable.Queue[String]()
  private def paStr(sink:String, src:String):String = {
    s"  assign $sink = $src;"
  }
  private def xmrIO(data:Data, sink:String, src:String): Unit = {
    data match {
      case b: Bundle => b.elements.foreach({case(n, d) => xmrIO(d, s"${sink}_$n", s"${src}_$n")})
      case v: Vec[Data] => v.zipWithIndex.foreach({case(d, i) => xmrIO(d, s"${sink}_$i", s"${src}_$i")})
      case _ => if(data.getWidth != 0) xmrQueue.addOne(paStr(sink, src))
    }
  }
  for((n ,d) <- io.probe.elements) {
    d match {
      case v: Vec[Data] => v.zipWithIndex.foreach({ case(d, i) =>
        val srcKey = s"difftest${n.capitalize}_$i"
        val srcName = CoreGateway.getOne(srcKey).pathName.replace(s"${xs.utils.GlobalData.prefix}CpuCluster", cc)
        val sinkName = s"probe_${n}_$i"
        xmrIO(d, sinkName, srcName)
      })
      case d: Data =>
        val srcKey = s"difftest${n.capitalize}"
        val srcName = CoreGateway.getOne(srcKey).pathName.replace(s"${xs.utils.GlobalData.prefix}CpuCluster", cc)
        val sinkName = s"probe_$n"
        xmrIO(d, sinkName, srcName)
    }
  }
  private val assignmentDecl = xmrQueue.mkString("\n")

  setInline(s"$desiredName.sv",
    s"""
       |module $desiredName (
       |$portDecl,
       |  output         reset,
       |  output         clock
       |);
       |$assignmentDecl
       |  assign reset = $cc.tile.core.reset;
       |  assign clock = $cc.tile.core.clock;
       |endmodule""".stripMargin)
}
