/***************************************************************************************
* Copyright (c) 2020-2021 Institute of Computing Technology, Chinese Academy of Sciences
* Copyright (c) 2020-2021 Peng Cheng Laboratory
*
* XiangShan is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

package lntest.top

import org.chipsalliance.cde.config.Parameters
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3._
import chisel3.util.experimental.BoringUtils
import chisel3.util.{MixedVec, ReadyValidIO}
import lntest.peripheral.SimJTAG
import org.chipsalliance.diplomacy.lazymodule._
import xs.utils.FileRegisters
import difftest._
import linknan.cluster.BlockTestIO
import linknan.devicetree.DeviceTreeGenerator
import linknan.generator.Generator
import linknan.soc.{LNTop, LinkNanParamsKey}
import linknan.utils.connectByName
import lntest.info.InfoGen
import org.chipsalliance.diplomacy.DisableMonitors
import xiangshan.XSCoreParamsKey
import xijiang.NodeType
import xijiang.tfb.TrafficBoardFileManager
import xs.utils.perf.{DebugOptionsKey, LogPerfHelper}
import xs.utils.stage.XsStage
import zhujiang.{NocIOHelper, ZJParametersKey}
import zhujiang.axi.{AxiBundle, AxiParams, AxiUtils, BaseAxiXbar, ExtAxiBundle}

class StMmioBridge(mstParams:Seq[AxiParams]) extends BaseAxiXbar(mstParams) {
  private def internal(addr:UInt): Bool = addr < 0x5000_0000L.U
  private def external(addr: UInt):Bool = 0x5000_0000L.U <= addr && addr < 0x8000_0000L.U
  val slvMatchersSeq = Seq(internal, external)
  initialize()
}

class SimNto1Bridge(mstParams:Seq[AxiParams]) extends BaseAxiXbar(mstParams) {
  val slvMatchersSeq = Seq(_ => true.B)
  initialize()
}

class SystemExtensionWrapper(slvP:AxiParams, mstP:AxiParams) extends BlackBox {
  val io = IO(new Bundle{
    val s_axi_cfg = Flipped(new AxiBundle(slvP))
    val m_axi_dma = new AxiBundle(mstP)
    val ext_intr = Output(UInt(256.W))
    val clock = Input(Clock())
    val reset = Input(AsyncReset())
  })
}

class SimTop(implicit val p: Parameters) extends Module with NocIOHelper {
  override def resetType = Module.ResetType.Asynchronous
  private val debugOpts = p(DebugOptionsKey)
  private val doBlockTest = p(LinkNanParamsKey).removeCore
  private val soc = Module(new LNTop)

  private val mmioXbar = if(doBlockTest) Module(new SimNto1Bridge(soc.cfgIO.map(_.params))) else Module(new StMmioBridge(soc.cfgIO.map(_.params)))
  mmioXbar.io.upstream.zip(soc.cfgIO).foreach({case(a, b) => a <> b})
  private val intCfgPort = if(doBlockTest) None else Some(mmioXbar.io.downstream.head)
  private val extCfgPort = if(doBlockTest) mmioXbar.io.downstream.head else mmioXbar.io.downstream.last

  private val l_simMMIO = if(doBlockTest) None else Some(LazyModule(new SimMMIO(intCfgPort.get.params, soc.dmaIO.head.params)))
  private val simMMIO = if(doBlockTest) None else Some(Module(l_simMMIO.get.module))

  val io = IO(new Bundle(){
    val simFinal = Option.when(p(DebugOptionsKey).EnableLuaScoreBoard)(Input(Bool()))
  })
  soc.dmaIO.foreach(_ := DontCare)
  val ddrDrv = Seq()
  val cfgDrv = if(doBlockTest) Seq(extCfgPort) else Seq()
  val dmaDrv = if(doBlockTest) soc.dmaIO.map(AxiUtils.getIntnl) else Seq()
  val ccnDrv = Seq()
  val hwaDrv = if(doBlockTest) {
    soc.hwaIO.map(AxiUtils.getIntnl)
  } else {
    soc.hwaIO.foreach(_ := DontCare)
    None
  }

  private val dmaPort = soc.dmaIO.filter(_.params.dataBits >= 256).head
  private val extraDev = if(doBlockTest) None else Some(Module(new SystemExtensionWrapper(extCfgPort.params, dmaPort.params)))
  extraDev.foreach(d => {
    d.io.s_axi_cfg <> extCfgPort
    dmaPort <> d.io.m_axi_dma
    d.io.clock := clock
    d.io.reset := reset
  })

  runIOAutomation()
  if(doBlockTest) {
    soc.io.ext_intr := 0.U
    dmaIO.foreach(InfoGen.addSaxi)
    hwaIO.foreach(InfoGen.addSaxi)
    cfgIO.foreach(InfoGen.addMaxi)
  } else {
    soc.io.ext_intr := simMMIO.get.io.intr | extraDev.map(_.io.ext_intr).getOrElse(0.U)
    val periCfg = simMMIO.get.cfg.head
    connectByName(periCfg.aw, intCfgPort.get.aw)
    connectByName(periCfg.ar, intCfgPort.get.ar)
    connectByName(periCfg.w, intCfgPort.get.w)
    connectByName(intCfgPort.get.r, periCfg.r)
    connectByName(intCfgPort.get.b, periCfg.b)
  }

  private val memXbar = Module(new SimNto1Bridge(soc.ddrIO.map(_.params)))
  memXbar.io.upstream.zip(soc.ddrIO).foreach({case(a, b) => a <> b})
  private val memPort = memXbar.io.downstream.head
  private val l_simAXIMem = LazyModule(new DummyDramMoudle(memPort.params))
  private val simAXIMem = Module(l_simAXIMem.module)
  private val memAxi = simAXIMem.axi.head
  connectByName(memAxi.aw, memPort.aw)
  connectByName(memAxi.ar, memPort.ar)
  connectByName(memAxi.w, memPort.w)
  connectByName(memPort.r, memAxi.r)
  connectByName(memPort.b, memAxi.b)

  private val cntDiv = p(LinkNanParamsKey).rtcDiv
  val cnt = RegInit((cntDiv - 1).U)
  val tick = cnt < (cntDiv / 2).U
  cnt := Mux(cnt === 0.U, (cntDiv - 1).U, cnt - 1.U)

  private val socReset = reset.asAsyncReset.asBool || soc.io.ndreset
  soc.io.rtc_clock := tick
  soc.io.noc_clock := clock
  soc.io.cluster_clocks.foreach(_ := clock)
  soc.io.reset := socReset.asAsyncReset
  soc.io.dft := DontCare
  soc.io.ramctl := DontCare
  soc.io.dft.lgc_rst_n := true.B
  soc.io.default_reset_vector := 0x10000000L.U
  soc.io.ci := 0.U
  soc.io.default_cpu_enable.foreach(_ := false.B)
  if(doBlockTest) {
    soc.io.default_cpu_enable.foreach(_ := true.B)
  } else {
    soc.io.default_cpu_enable(0) := true.B
  }

  if(doBlockTest) {
    soc.io.jtag.foreach( jtag => {
      jtag := DontCare
      jtag.reset := true.B.asAsyncReset
    })
  } else {
    soc.io.jtag.foreach( soc_jtag => {
      val success = Wire(Bool())
      val jtag = Module(new SimJTAG(tickDelay = 3))
      soc_jtag.reset := reset.asAsyncReset
      jtag.connect(soc_jtag.jtag, clock, reset.asBool, !reset.asBool, success, jtagEnable = true)
      soc_jtag.mfr_id := 0.U(11.W)
      soc_jtag.part_number := 0.U(16.W)
      soc_jtag.version := 0.U(4.W)
    })
  }

  if(p(DebugOptionsKey).EnableLuaScoreBoard) {
    val l2StrSeq = soc.ccns.zipWithIndex.map(n => s"[${n._2}] = { ${p(XSCoreParamsKey).L2NBanks}, ${n._1.cpuNum} }")
    val l2Str = l2StrSeq.reduce((a, b) => s"$a, $b")
    val nrHnf = p(ZJParametersKey).island.count(_.nodeType == NodeType.HF)
    val luaScb = Module(new LuaScoreboard(s"{ $l2Str }", nrHnf))
    luaScb.io.clock := clock
    luaScb.io.reset := reset.asBool
    luaScb.io.sim_final := io.simFinal.get
  }

  if(doBlockTest) {
    for(i <- soc.core.get.indices) {
      val ext = IO(new BlockTestIO(soc.core.get(i).params))
      if(soc.core.get(i).params.dcache) ext.suggestName(s"core_$i")
      else ext.suggestName(s"core_icn_$i")
      ext <> soc.core.get(i)
    }
  }


  if(!doBlockTest) {
    val difftest = DifftestModule.lntop_createTopIOs(BoringUtils.tapAndRead(soc.exit), BoringUtils.tapAndRead(soc.step))
    difftest.uart <> simMMIO.get.io.uart
  }

  DeviceTreeGenerator.simGenerate
  lntest.info.InfoGen.register(soc)
}

object SimGenerator extends App {
  val (config, firrtlOpts) = SimArgParser(args)
  xs.utils.GlobalData.prefix = config(LinkNanParamsKey).prefix
  difftest.GlobalData.prefix = config(LinkNanParamsKey).prefix
  (new XsStage).execute(firrtlOpts, Generator.firtoolOpts ++ Seq(
    ChiselGeneratorAnnotation(() => {
      DisableMonitors(p => new SimTop()(p))(config)
    })
  ))
  if(config(ZJParametersKey).tfbParams.isDefined) TrafficBoardFileManager.release("generated-src", "generated-src")(config)
  InfoGen.generate()(config)
  FileRegisters.write(filePrefix = config(LinkNanParamsKey).prefix + "LNTop.")
}
