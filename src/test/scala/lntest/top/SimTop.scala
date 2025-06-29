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
import chisel3.util.{MixedVec, ReadyValidIO}
import lntest.peripheral.SimJTAG
import org.chipsalliance.diplomacy.lazymodule._
import xs.utils.FileRegisters
import difftest._
import linknan.cluster.BlockTestIO
import linknan.devicetree.DeviceTreeGenerator
import linknan.generator.Generator
import linknan.soc.{LNTop, LinkNanParamsKey}
import lntest.info.InfoGen
import org.chipsalliance.diplomacy.DisableMonitors
import xiangshan.XSCoreParamsKey
import xijiang.NodeType
import xijiang.tfb.TrafficBoardFileManager
import xs.utils.perf.{DebugOptionsKey, LogPerfHelper}
import xs.utils.stage.XsStage
import zhujiang.{NocIOHelper, ZJParametersKey}
import zhujiang.axi.{AxiBundle, AxiParams, AxiUtils, BaseAxiXbar, ExtAxiBundle}

class MasterBridge(mstParams:Seq[AxiParams]) extends BaseAxiXbar(mstParams) {
  val slvMatchersSeq = Seq(_ => true.B)
  initialize()
}

class SimTop(implicit val p: Parameters) extends Module with NocIOHelper {
  override def resetType = Module.ResetType.Asynchronous
  private val debugOpts = p(DebugOptionsKey)
  private val doBlockTest = p(LinkNanParamsKey).removeCore
  private val soc = Module(new LNTop)

  private def genAxiMstPort(nocPorts:Seq[ExtAxiBundle]):AxiBundle = {
    val intnls = nocPorts.map(AxiUtils.getIntnl)
    val xbar = Module(new MasterBridge(intnls.map(_.params)))
    xbar.io.upstream.zip(intnls).foreach({case(a, b) => a <> b})
    xbar.io.downstream.head
  }

  private val cfgPort = genAxiMstPort(soc.cfgIO)
  private val memPort = genAxiMstPort(soc.ddrIO)

  private val l_simMMIO = if(doBlockTest) None else Some(LazyModule(new SimMMIO(cfgPort.params, soc.dmaIO.head.params)))
  private val simMMIO = if(doBlockTest) None else Some(Module(l_simMMIO.get.module))

  val io = IO(new Bundle(){
    val simFinal = Option.when(p(DebugOptionsKey).EnableLuaScoreBoard)(Input(Bool()))
  })
  soc.hwaIO.foreach(_ := DontCare)
  val ddrDrv = Seq()
  val cfgDrv = Seq(cfgPort)
  val dmaDrv = soc.dmaIO.map(AxiUtils.getIntnl)
  val ccnDrv = Seq()
  val hwaDrv = None

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

  if(doBlockTest) {
    soc.io.ext_intr := 0.U
    runIOAutomation()
    dmaIO.foreach(InfoGen.addSaxi)
    cfgIO.foreach(InfoGen.addMaxi)
  } else {
    dmaDrv.foreach(_ := DontCare)
    soc.io.ext_intr := simMMIO.get.io.intr
    val periCfg = simMMIO.get.cfg.head
    val periDma = simMMIO.get.dma.head
    val dmaMain = dmaDrv.filter(_.params.dataBits == 256).head

    connByName(periCfg.aw, cfgPort.aw)
    connByName(periCfg.ar, cfgPort.ar)
    connByName(periCfg.w, cfgPort.w)
    connByName(cfgPort.r, periCfg.r)
    connByName(cfgPort.b, periCfg.b)

    connByName(dmaMain.aw, periDma.aw)
    connByName(dmaMain.ar, periDma.ar)
    connByName(dmaMain.w, periDma.w)
    connByName(periDma.r, dmaMain.r)
    connByName(periDma.b, dmaMain.b)
  }

  private val l_simAXIMem = LazyModule(new DummyDramMoudle(memPort.params))
  private val simAXIMem = Module(l_simAXIMem.module)
  private val memAxi = simAXIMem.axi.head
  connByName(memAxi.aw, memPort.aw)
  connByName(memAxi.ar, memPort.ar)
  connByName(memAxi.w, memPort.w)
  connByName(memPort.r, memAxi.r)
  connByName(memPort.b, memAxi.b)

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
    val difftestMacros = Seq(
      s"DEBUG_MEM_BASE 0x${p(LinkNanParamsKey).debugBase.toHexString}",
      s"DEFAULT_EMU_RAM_SIZE 0x${(8L * 1024 * 1024 * 1024).toHexString}UL"
    )
    val difftest = DifftestModule.finish("XiangShan", difftestMacros)
    difftest.uart <> simMMIO.get.io.uart  // workaround for kmh difftest wrapper
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
