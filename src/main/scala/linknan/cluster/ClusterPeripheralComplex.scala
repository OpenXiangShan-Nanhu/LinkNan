package linknan.cluster
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy.LazyModule
import linknan.cluster.interconnect.PeriXBar
import org.chipsalliance.cde.config.Parameters
import zhujiang.tilelink.TilelinkParams

class ImiscWrapper(implicit p:Parameters) extends LazyModule {

}

class ClusterPeripheralComplex(tlParams: Seq[TilelinkParams]) extends Module{
}
