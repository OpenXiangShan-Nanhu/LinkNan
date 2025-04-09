package linknan.devicetree
import coupledL2.L2ParamKey
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import xiangshan.XSCoreParamsKey
import zhujiang.ZJParametersKey

final case class L3CacheNode(
)(implicit p:Parameters) extends DeviceNode(
  name = "l3cache",
  label = "L3",
  children = Nil,
  properties = List(
    Property("compatible", StringValue("cache")),
    Property("cache-unified", FlagValue()),
    Property("cache-size", IntegerValue(p(ZJParametersKey).cacheSizeInB)),
    Property("cache-sets", IntegerValue(p(ZJParametersKey).cacheSizeInB / 64 / p(ZJParametersKey).cacheWays)),
    Property("cache-block-size", IntegerValue(64)),
    Property("cache-level", IntegerValue(3))
  )
)

final case class L2CacheNode(
  id:Int
)(implicit p:Parameters) extends DeviceNode(
  name = "l2cache",
  label = s"l2_$id",
  children = Nil,
  properties = {
    val coreP = p(XSCoreParamsKey)
    val l2P = p(L2ParamKey)
    List(
      Property("compatible", StringValue("cache")),
      Property("cache-unified", FlagValue()),
      Property("cache-size", IntegerValue(l2P.sets * l2P.ways * l2P.blockBytes * coreP.L2NBanks)),
      Property("cache-sets", IntegerValue(l2P.sets * coreP.L2NBanks)),
      Property("cache-block-size", IntegerValue(l2P.blockBytes)),
      Property("cache-level", IntegerValue(2)),
      Property("next-level-cache", ReferenceValue("L3")),
    )
  }
)

final case class InterruptControllerNode(
  id:Int,
) extends DeviceNode(
  name = s"intc",
  label = s"cpu${id}_intc",
  children = Nil,
  properties = List(
    Property("#address-cells", IntegerValue(1)),
    Property("#size-cells", IntegerValue(0)),
    Property("#interrupt-cells", IntegerValue(1)),
    Property("interrupt-controller", FlagValue()),
    Property("compatible", StringValue("riscv,cpu-intc")),
  ),
)

final case class MtimerNode(
  id:Long,
  harts: Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"mtimer@${((id << p(ZJParametersKey).cpuSpaceBits) + 0x2000L).toHexString}",
  label = s"mtimer_$id",
  children = Nil,
  properties = {
    val base = id << p(ZJParametersKey).cpuSpaceBits
    val regList = List(RegValue(base + 0x2000L, 8), RegValue(base + 0x2008L, 8))
    val regNameList = List(StringValue("mtimecmp"), StringValue("mtime"))
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${id + i}_intc", 7))
    List(
      Property("compatible", StringValue("riscv,aclint-mtimer")),
      Property("reg", PropertyValues(regList)),
      Property("reg-names", PropertyValues(regNameList)),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
)

final case class PpuNode(
  id:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"ppu@${(id << p(ZJParametersKey).cpuSpaceBits).toHexString}",
  label = s"ppu_$id",
  children = Nil,
  properties = {
    val base = (id << p(ZJParametersKey).cpuSpaceBits)
    val regList = List(
      RegValue(base + 0x0000L, 8),
      RegValue(base + 0x1000L, 4),
      RegValue(base + 0x1004L, 4),
      RegValue(base + 0x1008L, 4),
      RegValue(base + 0x100CL, 4),
    )
    val regNameList = List(
      StringValue("CBAR"),
      StringValue("PWPR"),
      StringValue("PWSR"),
      StringValue("IMR"),
      StringValue("IPR")
    )
    List(
      Property("compatible", StringValue("bosc,linknan-ppu")),
      Property("reg", PropertyValues(regList)),
      Property("reg-names", PropertyValues(regNameList))
    )
  },
)

final case class MswiNode(
  harts:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"mswi@${p(LinkNanParamsKey).mswiBase.toHexString}",
  label = s"mswi",
  children = Nil,
  properties = {
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${i}_intc", 3))
    List(
      Property("compatible", StringValue("riscv,aclint-mswi")),
      Property("reg", RegValue(p(LinkNanParamsKey).mswiBase, harts * 0x4)),
      Property("reg-names", StringValue("msip")),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
)

final case class PlicNode(
  harts:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"plic@${p(LinkNanParamsKey).plicBase.toHexString}",
  label = s"plic",
  children = Nil,
  properties = {
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${i}_intc", 11)) ++ Seq.tabulate(harts)(i => (s"cpu${i}_intc", 9))
    List(
      Property("#address-cells", IntegerValue(2)),
      Property("#size-cells", IntegerValue(1)),
      Property("compatible", StringValue("riscv,plic0")),
      Property("reg", RegValue(p(LinkNanParamsKey).plicBase, 0x400_0000)),
      Property("reg-names", StringValue("control")),
      Property("interrupt-controller", FlagValue()),
      Property("#interrupt-cells", IntegerValue(1)),
      Property("riscv,ndev", IntegerValue(p(LinkNanParamsKey).nrExtIntr)),
      Property("riscv,max-priority", IntegerValue(7)),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
)

final case class RefMtimerNode(
  base:Long
)(implicit p:Parameters) extends DeviceNode(
  name = s"ref_mtimer@${(base + 0x8).toHexString}",
  label = s"ref_mtimer",
  children = Nil,
  properties = {
    List(
      Property("compatible", StringValue("riscv,aclint-mtimer")),
      Property("reg", RegValue(base + 0x8, 8)),
      Property("reg-names", StringValue("mtime")),
    )
  },
)

final case class DebugModuleNode(
  harts:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"debug@${p(LinkNanParamsKey).debugBase.toHexString}",
  label = "DEBUG",
  children = Nil,
  properties = {
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${i}_intc", 65535))
    List(
      Property("compatible", PropertyValues(List(StringValue("sifive,debug-013"), StringValue("riscv,debug-013")))),
      Property("interrupts-extended", IntrValue(intrSeq)),
      Property("reg", RegValue(p(LinkNanParamsKey).debugBase, 0x1000)),
      Property("reg-names", StringValue("control")),
    )
  }
)

final case class CoreNode(
  id:Int,
)(implicit p:Parameters) extends DeviceNode(
  name = s"cpu@$id",
  label = s"cpu$id",
  children = List(L2CacheNode(id), InterruptControllerNode(id)),
  properties = {
    val coreP = p(XSCoreParamsKey)
    val dtlbP = coreP.dtlbParameters
    val itlbP = coreP.itlbParameters
    val icacheP = coreP.icacheParameters
    val dcacheP = coreP.dcacheParametersOpt
    val res = List(
      Property("device-type", StringValue("cpu")),
      Property("reg", IntegerValue(id)),
      Property("status", StringValue(if(id == 0) "ok" else "disabled")),
      Property("compatible", StringValue("bosc,nanhu-v5")),
      Property("riscv,isa", StringValue("rv64imafdc")),
      Property("cache-op-block-size", IntegerValue(icacheP.blockBytes)),
      Property("reservation-granule-size", IntegerValue(icacheP.blockBytes)),
      Property("mmu-type", StringValue(s"riscv,sv48")),
      Property("tlb-split", FlagValue()),
      Property("d-tlb-size", IntegerValue(dtlbP.NSets * dtlbP.NWays)),
      Property("d-tlb-sets", IntegerValue(1)),
      Property("i-tlb-size", IntegerValue(itlbP.NSets * itlbP.NWays)),
      Property("i-tlb-sets", IntegerValue(1)),
      Property("i-cache-size", IntegerValue(icacheP.nWays * icacheP.nSets * icacheP.blockBytes)),
      Property("i-cache-sets", IntegerValue(icacheP.nSets)),
      Property("i-cache-block-size", IntegerValue(icacheP.blockBytes)),
    )
    if(dcacheP.isDefined) {
      res ++ List(
        Property("d-cache-size", IntegerValue(dcacheP.get.nWays * dcacheP.get.nSets * dcacheP.get.blockBytes)),
        Property("d-cache-sets", IntegerValue(dcacheP.get.nSets)),
        Property("d-cache-block-size", IntegerValue(dcacheP.get.blockBytes)),
      )
    } else {
      res
    }
  }
)

final case class CpuNode(
  cpuCount: Int,
)(implicit p:Parameters) extends DeviceNode(
  name = "cpus",
  label = "",
  properties = List(
    Property("timebase-frequency", IntegerValue(p(LinkNanParamsKey).rtcFreq)),
    Property("#address-cells", IntegerValue(1)),
    Property("#size-cells", IntegerValue(0)),
  ),
  children = List.tabulate(cpuCount)(id => CoreNode(id)) :+ L3CacheNode()
)

final case class SocNode(
  cpuCount: Int,
)(implicit p:Parameters) extends DeviceNode(
  name = "soc",
  label = "",
  properties = List(
    Property("timebase-frequency", IntegerValue(p(LinkNanParamsKey).rtcFreq)),
    Property("#address-cells", IntegerValue(2)),
    Property("#size-cells", IntegerValue(1)),
    Property("compatible", StringValue("simple-bus"))
  ),
  children = List.tabulate(cpuCount)(id => MtimerNode(
    id = id,
    harts = 1
  )) ++
    List.tabulate(cpuCount)(id => PpuNode(id = id)) ++
    List(MswiNode(cpuCount), PlicNode(cpuCount), RefMtimerNode(0x2000L), DebugModuleNode(cpuCount))
)

final case class MemNode(
)(implicit p:Parameters) extends DeviceNode(
  name = s"memory@${p(LinkNanParamsKey).memBase.toHexString}",
  label = "",
  properties = List(
    Property("device_type", StringValue("memory")),
    Property("reg", RegValue(p(LinkNanParamsKey).memBase, p(LinkNanParamsKey).memSizeInB, 2, 2)),
  ),
)

final case class ChosenNode(
)extends DeviceNode(
  name = s"chosen",
  label = "",
  properties = List(
    Property("device_type", StringValue("console=hvc0 earlycon=sbi "))
  ),
)

final case class SerialNode(
)extends DeviceNode (
  name = s"serial@40600000",
  label = "",
  properties = List(
    Property("compatible", StringValue("xlnx,xps-uartlite-1.00.a")),
    Property("interrupt-parent", ReferenceValue("plic")),
    Property("interrupts", IntegerValue(3)),
    Property("current-speed", IntegerValue(115200)),
    Property("reg", RegValue(0x40600000, 0x1000)),
    Property("reg-names", StringValue("control"))
  ),
)