package linknan.cluster.hub

import aia.{CSRToIMSICBundle, IMSICParams, IMSICToCSRBundle, TLIMSIC}
import chisel3._
import chisel3.util._
import freechips.rocketchip.diplomacy.IdRange
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.tilelink.{TLBuffer, TLClientNode, TLMasterParameters, TLMasterPortParameters}
import linknan.cluster.hub.interconnect.{ClusterPeriParams, PeriXBar}
import linknan.cluster.hub.peripheral._
import linknan.cluster.power.controller.{PcsmCtrlIO, PowerControllerTop, PowerMode, devActiveBits}
import linknan.cluster.power.pchannel.PChannel
import linknan.soc.LinkNanParamsKey
import org.chipsalliance.cde.config.Parameters
import zhujiang.ZJBundle
import zhujiang.tilelink.{TLULBundle, TilelinkParams}

class ImsicBundle(implicit p:Parameters) extends Bundle {
  val fromCpu = Flipped(new CSRToIMSICBundle(p(LinkNanParamsKey).imiscParams))
  val toCpu = new IMSICToCSRBundle(p(LinkNanParamsKey).imiscParams)
}

class ImsicWrapper(mst:TilelinkParams)(implicit p:Parameters) extends LazyModule {
  private val imiscParams = p(LinkNanParamsKey).imiscParams
  private val inner = LazyModule(new TLIMSIC(imiscParams, 8))
  private val clientParameters = TLMasterPortParameters.v1(
    clients = Seq(TLMasterParameters.v1(
      "cfg",
      sourceId = IdRange(0, 1 << mst.sourceBits)
    ))
  )
  private val clientNode = TLClientNode(Seq(clientParameters))
  inner.fromMem :*= TLBuffer() :*= clientNode

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val io = IO(new Bundle{
      val tls = Flipped(new TLULBundle(mst))
      val imsic = new ImsicBundle
    })

    private def connByName(sink:ReadyValidIO[Bundle], src:ReadyValidIO[Bundle]):Unit = {
      sink.valid := src.valid
      src.ready := sink.ready
      sink.bits := DontCare
      val recvMap = sink.bits.elements.map(e => (e._1.toLowerCase, e._2))
      val sendMap = src.bits.elements.map(e => (e._1.toLowerCase, e._2))
      for((name, data) <- recvMap) {
        if(sendMap.contains(name)) data := sendMap(name).asTypeOf(data)
      }
    }

    connByName(clientNode.out.head._1.a, io.tls.a)
    connByName(io.tls.d, clientNode.out.head._1.d)
    io.imsic.toCpu := inner.module.toCSR
    inner.module.fromCSR := io.imsic.fromCpu
  }
}

class CpuCtlBundle(implicit p:Parameters) extends ZJBundle {
  val imsic = new ImsicBundle
  val bootAddr = Output(UInt(64.W))
  val pchn = new PChannel(devActiveBits, PowerMode.powerModeBits)
  val pcsm = new PcsmCtrlIO
  val defaultBootAddr = Input(UInt(64.W))
  val defaultEnable = Input(Bool())
  val coreId = Input(UInt(cpuIdBits.W))
  val blockProbe = Output(Bool())
}

class CsuCtlBundle(implicit p:Parameters) extends ZJBundle {
  val pchn = new PChannel(devActiveBits, PowerMode.powerModeBits)
  val pcsm = new PcsmCtrlIO
}

class ClusterCtlBundle(implicit p:Parameters) extends ZJBundle {
  val clusterClkEn = Output(Bool())
  val pllCfg = Output(Vec(8, UInt(32.W)))
  val pllLock = Input(Bool())
}

class ClusterPeriBlock(tlParams: Seq[TilelinkParams], coreNum:Int)(implicit p:Parameters) extends Module {
  private val privateSeq = Seq.tabulate(coreNum)(i => Seq(
    ClusterPeriParams(s"imisc_$i", Seq((0x0000, 0x8000), (0x8000, 0x9000)), Some(i)),
    ClusterPeriParams(s"cpu_boot_ctl_$i", Seq((0x9000, 0xA000)), Some(i)),
    ClusterPeriParams(s"cpu_pwr_ctl_$i", Seq((0xA000, 0xB000)), Some(i))
  )).reduce(_ ++ _)

  private val sharedSeq = Seq(
    ClusterPeriParams("pll", Seq((0x1_0000, 0x1_1000)), None),
    ClusterPeriParams("csu_pwr_ctl", Seq((0x1_1000, 0x1_2000)), None),
  )
  private val periSeq = privateSeq ++ sharedSeq
  private val periXBar = Module(new PeriXBar(tlParams, periSeq, coreNum))

  private val imsicSeq = Seq.tabulate(coreNum) { i=>
    val imsic = LazyModule(new ImsicWrapper(periXBar.io.downstream.head.params))
    val _imisc = Module(imsic.module)
    _imisc.suggestName(s"imisc_$i")
    (i, _imisc)
  }
  private val cpuBootCtlSeq = Seq.tabulate(coreNum) { i=>
    val cpuCtl = Module(new CpuBootCtrl(periXBar.io.downstream.head.params))
    cpuCtl.suggestName(s"cpu_boot_ctl_$i")
    (i, cpuCtl)
  }
  private val cpuPwrCtlSeq = Seq.tabulate(coreNum) { i=>
    val cpuCtl = Module(new PowerControllerTop(periXBar.io.downstream.head.params, false))
    cpuCtl.suggestName(s"cpu_pwr_ctl_$i")
    (i, cpuCtl)
  }

  private val pllCtl = Module(new ClusterPLL(periXBar.io.downstream.head.params))
  private val csuPwrCtl = Module(new PowerControllerTop(periXBar.io.downstream.head.params, true))
  private val clusterClkEn = cpuPwrCtlSeq.map(_._2.io.pcsmCtrl.clkEn).reduce(_ || _) || csuPwrCtl.io.pcsmCtrl.clkEn

  private val downstreams = periSeq.zip(periXBar.io.downstream)
  private def cpuDevConn(tlSeq: Seq[(Int, TLULBundle)], pfx:String):Unit = {
    for((i, tls) <- tlSeq) {
      val tlm = downstreams.filter(_._1.name == s"$pfx$i").map(_._2).head
      tls <> tlm
    }
  }
  cpuDevConn(imsicSeq.map(e => (e._1, e._2.io.tls)), "imisc_")
  cpuDevConn(cpuBootCtlSeq.map(e => (e._1, e._2.tls)), "cpu_boot_ctl_")
  cpuDevConn(cpuPwrCtlSeq.map(e => (e._1, e._2.io.tls)), "cpu_pwr_ctl_")

  pllCtl.tls <> downstreams.filter(_._1.name == s"pll").map(_._2).head
  csuPwrCtl.io.tls <> downstreams.filter(_._1.name == s"csu_pwr_ctl").map(_._2).head

  val io = IO(new Bundle{
    val tls = MixedVec(tlParams.map(t => Flipped(new TLULBundle(t))))
    val cpu = Vec(coreNum, new CpuCtlBundle)
    val csu = new CsuCtlBundle
    val cluster = new ClusterCtlBundle
  })
  io.cluster.clusterClkEn := RegNext(clusterClkEn)
  pllCtl.io.lock := io.cluster.pllLock
  io.cluster.pllCfg := pllCtl.io.cfg
  io.csu.pchn <> csuPwrCtl.io.pChnMst
  io.csu.pcsm <> csuPwrCtl.io.pcsmCtrl
  csuPwrCtl.io.powerOnState := Mux(io.cpu.map(_.defaultEnable).reduce(_ | _), PowerMode.ON, PowerMode.OFF)
  csuPwrCtl.io.deactivate := false.B

  periXBar.cores.zip(io.cpu).foreach({case(a, b) => a := b.coreId})
  io.tls.zip(periXBar.io.upstream).foreach {case(a, b) => a <> b}

  for(i <- 0 until coreNum) {
    io.cpu(i).imsic <> imsicSeq(i)._2.io.imsic
    io.cpu(i).bootAddr := cpuBootCtlSeq(i)._2.io.cpuBootAddr
    io.cpu(i).pchn <> cpuPwrCtlSeq(i)._2.io.pChnMst
    io.cpu(i).pcsm <> cpuPwrCtlSeq(i)._2.io.pcsmCtrl
    io.cpu(i).blockProbe := RegNext(cpuPwrCtlSeq(i)._2.io.blockProbe)
    cpuBootCtlSeq(i)._2.io.defaultBootAddr := io.cpu(i).defaultBootAddr
    cpuPwrCtlSeq(i)._2.io.powerOnState := Mux(io.cpu(i).defaultEnable, PowerMode.ON, PowerMode.OFF)
    cpuPwrCtlSeq(i)._2.io.deactivate := csuPwrCtl.io.mode === PowerMode.OFF
  }
}
