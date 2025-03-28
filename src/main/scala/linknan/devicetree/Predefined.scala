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
    Property("#interrupt-cells", IntegerValue(1)),
    Property("interrupt-controller", FlagValue()),
    Property("compatible", StringValue("riscv,cpu-intc")),
  ),
)

final case class MtimerNode(
  id:Int,
  mtimeBase:Long,
  mtimecmpBase: Option[Long],
  harts: Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"mtimer@$id",
  label = s"MTIMER_$id",
  children = Nil,
  properties = {
    val regList = if(mtimecmpBase.isDefined) {
      List(RegValue(mtimeBase, 8), RegValue(mtimecmpBase.get, 8))
    } else {
      List(RegValue(mtimeBase, 8))
    }
    val regNameList = if(mtimecmpBase.isDefined) {
      List(StringValue("mtime"), StringValue("mtimecmp"))
    } else {
      List(StringValue("mtime"))
    }
    val intrSeq = if(mtimecmpBase.isDefined) {
      Seq.tabulate(harts)(i => (s"cpu${id + i}_intc", 7))
    } else {
      Nil
    }
    List(
      Property("#address-cells", IntegerValue(2)),
      Property("#size-cells", IntegerValue(1)),
      Property("compatible", StringValue("riscv,aclint-mtimer")),
      Property("reg", PropertyValues(regList)),
      Property("reg-names", PropertyValues(regNameList)),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
)

final case class MswiNode(
  harts:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"mswi",
  label = s"MSWI",
  children = Nil,
  properties = {
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${i}_intc", 3))
    List(
      Property("#address-cells", IntegerValue(2)),
      Property("#size-cells", IntegerValue(1)),
      Property("compatible", StringValue("riscv,aclint-mswi")),
      Property("reg", RegValue(p(LinkNanParamsKey).mswiBase, harts)),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
)

final case class PlicNode(
  harts:Int
)(implicit p:Parameters) extends DeviceNode(
  name = s"plic",
  label = s"PLIC",
  children = Nil,
  properties = {
    val intrSeq = Seq.tabulate(harts)(i => (s"cpu${i}_intc", 11)) ++ Seq.tabulate(harts)(i => (s"cpu${i}_intc", 9))
    List(
      Property("#address-cells", IntegerValue(2)),
      Property("#size-cells", IntegerValue(1)),
      Property("compatible", StringValue("riscv,plic0")),
      Property("reg", RegValue(p(LinkNanParamsKey).plicBase, 0x400_0000)),
      Property("interrupt-controller", FlagValue()),
      Property("#interrupt-cells", IntegerValue(1)),
      Property("riscv,ndev", IntegerValue(p(LinkNanParamsKey).nrExtIntr)),
      Property("riscv,max-priority", IntegerValue(7)),
      Property("interrupts-extended", IntrValue(intrSeq))
    )
  },
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
      Property("enable-method", StringValue("bosc,linknan")),
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

final case class RootNode(
)(implicit p:Parameters) extends DeviceNode(
  name = "/"
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
    mtimeBase = (id << p(ZJParametersKey).cpuSpaceBits) + 0x2000L,
    mtimecmpBase = Some((id << p(ZJParametersKey).cpuSpaceBits) + 0x2008L),
    harts = 1
  )) ++ List(MswiNode(cpuCount), PlicNode(cpuCount))
)