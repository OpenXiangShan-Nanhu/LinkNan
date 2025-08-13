package lntest.top

import chisel3.Module
import freechips.rocketchip.amba.axi4.{AXI4MasterNode, AXI4MasterParameters, AXI4MasterPortParameters, AXI4SlaveNode, AXI4SlaveParameters, AXI4SlavePortParameters, AXI4Xbar}
import freechips.rocketchip.diplomacy.{AddressSet, IdRange, RegionType, TransferSizes}
import freechips.rocketchip.resources.MemoryDevice
import linknan.generator.AddrConfig
import org.chipsalliance.diplomacy.lazymodule._
import linknan.soc.LinkNanParamsKey
import lntest.peripheral.{AXI4MemorySlave, AXI4RAMWrapper}
import org.chipsalliance.cde.config.Parameters
import xs.utils.perf.DebugOptionsKey
import zhujiang.ZJParametersKey
import zhujiang.axi.AxiParams

class DummyDramMoudle(memParams: AxiParams)(implicit p: Parameters) extends LazyModule{
  private val maw = p(ZJParametersKey).requestAddrBits - 1

  private val memDplmcMstParams = AXI4MasterPortParameters(
    masters = Seq(
      AXI4MasterParameters(
        name = "mem",
        id = IdRange(0, 1 << memParams.idBits)
      )
    )
  )

  private val memDplmcSlvParams = AXI4SlavePortParameters (
    slaves = Seq(
      AXI4SlaveParameters(
        address = AddrConfig.memFullAddrSet,
        regionType = RegionType.UNCACHED,
        executable = true,
        supportsRead = TransferSizes(1, 64),
        supportsWrite = TransferSizes(1, 64),
        interleavedId = Some(0),
        resources = (new MemoryDevice).reg("mem")
      )
    ),
    beatBytes = memParams.dataBits / 8
  )

  private val pciMemSize = 64L * 1024 * 1024
  private val pciDplmcSlvParams = AXI4SlavePortParameters (
    slaves = Seq(
      AXI4SlaveParameters(
        address = Seq(AddressSet(AddrConfig.mem_nc.head._1, pciMemSize - 1)),
        regionType = RegionType.UNCACHED,
        executable = true,
        supportsRead = TransferSizes(1, 64),
        supportsWrite = TransferSizes(1, 64),
        interleavedId = Some(0),
        resources = (new MemoryDevice).reg("mem")
      )
    ),
    beatBytes = memParams.dataBits / 8
  )

  private val mstNode = AXI4MasterNode(Seq(memDplmcMstParams))
  private val memNode = AXI4SlaveNode(Seq(memDplmcSlvParams))
  private val pciNode = AXI4SlaveNode(Seq(pciDplmcSlvParams))
  private val xbar = LazyModule(new AXI4Xbar)
  xbar.node :=* mstNode
  memNode :*= xbar.node
  pciNode :*= xbar.node
  lazy val module = new Impl

  class Impl extends LazyModuleImp(this) {

    val axi = mstNode.makeIOs()

    private val l_simAXIMem = AXI4MemorySlave(
      memNode,
      8L * 1024 * 1024 * 1024,
      useBlackBox = true,
      dynamicLatency = p(DebugOptionsKey).UseDRAMSim,
      pureDram = p(LinkNanParamsKey).removeCore
    )
    private val extraMem = LazyModule(new AXI4RAMWrapper(
      slave = pciNode,
      memByte = pciMemSize,
      useBlackBox = false
    ))

    private val simAXIMem = Module(l_simAXIMem.module)
    private val simAXIPci = Module(extraMem.module)
    l_simAXIMem.io_axi4.head <> memNode.in.head._1
    extraMem.io_axi4.head <> pciNode.in.head._1
  }
}
