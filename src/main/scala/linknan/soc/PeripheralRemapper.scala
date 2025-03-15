package linknan.soc
import chisel3._
import chisel3.util._
import linknan.cluster.hub.interconnect.ClusterAddrBundle
import org.chipsalliance.cde.config.Parameters
import xijiang.NodeType
import zhujiang.{ZJBundle, ZJModule}

/*
CI ID Bits = 4
CPU ID Bits = 4
Hart ID Bits = [ci, cpu]
CPU Space =     chip * 0x10_0000_0000 + cpu * 0x10_0000
APLIC M_ADDR:   0x00_0000
APLIC SG_ADDR:  0x10_0000
IMSIC SG_LA:    0x00_0000 + hart * 0x8000 (max addr: 0x7F_FFFF)
IMSIC M_LA:     0x80_0000 + hart * 0x1000 (max addr: 0x8F_FFFF)
IMSIC SG_PA:    chip * 0x10_0000_0000 + cpu * 0x10_0000 + 0x0000 (32 KiB)
IMSIC M_PA:     chip * 0x10_0000_0000 + cpu * 0x10_0000 + 0x8000 (4  KiB)
*/

class LogicAddrBundle(implicit p:Parameters) extends ZJBundle {
  private val remapBaseMaskBits = p(LinkNanParamsKey).remapBaseMaskBits
  val ci = UInt(ciIdBits.W)
  val tag = UInt((raw - ciIdBits - remapBaseMaskBits).W)
  val dev = UInt(remapBaseMaskBits.W)
  require(this.getWidth == raw)
}

class PeripheralRemapper(implicit p:Parameters) extends ZJModule {
  private val io = IO(new Bundle{
    val la = Input(UInt(raw.W))
    val pa = Output(UInt(raw.W))
  })
  require(zjParams.cpuSpaceBits >= 20)
  private val lnParams = p(LinkNanParamsKey)
  private val la = io.la.asTypeOf(new LogicAddrBundle)
  private val nla = Wire(UInt(raw.W))
  private val remap = false.B //TODO: Change this for new SoC
  io.pa := Mux(remap, nla, io.la)

  private def AddrMapper(base:Int, hartOff:Int, clusterBase:Int)(devAddr:UInt): (Bool, UInt) = {
    val hartMaskBits = hartOff + clusterIdBits
    val hartMask = (1 << hartMaskBits) - 1
    require((base & hartMask) == 0, s"align check failed! base: ${base.toHexString} mask: ${hartMask.toHexString}")
    val hart = devAddr(clusterIdBits - 1 + hartOff, hartOff)
    val ci = hart(clusterIdBits - 1, cpuIdBits)
    val cpu = hart(ciIdBits - 1, 0)
    val doRemap = (base >> hartMaskBits).U === (devAddr >> hartMaskBits).asUInt
    val phyAddr = Wire(new ClusterAddrBundle)
    phyAddr.ci := ci
    phyAddr.tag := 0.U
    phyAddr.cpu := cpu
    phyAddr.dev := clusterBase.U | devAddr(hartOff - 1, 0)
    (doRemap, phyAddr.asUInt)
  }

  private val mapperSeq = Seq[UInt => (Bool, UInt)](
    AddrMapper(lnParams.imiscSgBase, 15, 0x0_0000), //IMSIC SG_LA
    AddrMapper(lnParams.imsicMBase, 12, 0x0_8000), //IMSIC M_LA
  )
  private val mapRes = mapperSeq.map(m => m(la.dev))
  nla := Mux1H(mapRes)
}

object PeripheralRemapper {
  def apply(addr:UInt, p:Parameters):UInt = {
    val remapper = Module(new PeripheralRemapper()(p))
    remapper.io.la := addr
    remapper.io.pa
  }
}