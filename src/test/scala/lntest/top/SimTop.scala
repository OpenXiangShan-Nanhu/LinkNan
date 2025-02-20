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

import SimpleL2.Configs.L2ParamKey
import chisel3.Module.ResetType
import org.chipsalliance.cde.config.Parameters
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3._
import chisel3.util.{MixedVec, ReadyValidIO}
import lntest.peripheral.SimJTAG
import org.chipsalliance.diplomacy.lazymodule._
import xs.utils.{FileRegisters, GTimer}
import difftest._
import circt.stage.ChiselStage
import linknan.cluster.BlockTestIO
import linknan.generator.{DcacheKey, Generator, MiscKey, TestIoOptionsKey}
import linknan.soc.LNTop
import org.chipsalliance.diplomacy.DisableMonitors
import org.chipsalliance.diplomacy.nodes.MonitorsEnabled
import xijiang.NodeType
import xijiang.tfb.TrafficBoardFileManager
import xs.utils.perf.DebugOptionsKey
import xs.utils.tl.{TLUserKey, TLUserParams}
import zhujiang.ZJParametersKey
import zhujiang.axi.{AxiBundle, AxiUtils}

class SimTop(implicit p: Parameters) extends Module {
  override def resetType = Module.ResetType.Asynchronous
  private val debugOpts = p(DebugOptionsKey)
  private val doBlockTest = p(TestIoOptionsKey).doBlockTest
  private val hasCsu = p(TestIoOptionsKey).hasCsu
  private val soc = Module(new LNTop)
  private val cfgMain = soc.cfgIO.filter(_.params.attr == "main").head
  private val dmaMain = soc.dmaIO.filter(_.params.attr == "main").head
  private val l_simMMIO = if(doBlockTest) None else Some(LazyModule(new SimMMIO(cfgMain.params, dmaMain.params)))
  private val simMMIO = if(doBlockTest) None else Some(Module(l_simMMIO.get.module))

  val io = IO(new Bundle(){
    val simFinal = if(hasCsu) Some(Input(Bool())) else None
    val logCtrl = if(doBlockTest) None else Some(new LogCtrlIO)
    val perfInfo = if(doBlockTest) None else Some(new PerfInfoIO)
    val uart = if(doBlockTest) None else Some(new UARTIO)
    val dma = if(doBlockTest) Some(MixedVec(soc.dmaIO.map(drv => Flipped(new AxiBundle(drv.params))))) else None
    val cfg = if(doBlockTest) Some(new AxiBundle(cfgMain.params)) else None
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

  soc.cfgIO.filterNot(_.params.attr == "main").foreach(_ := DontCare)
  if(doBlockTest) {
    io.dma.get.zip(soc.dmaIO).foreach({case(a, b) => a <> b})
    io.cfg.get <> cfgMain
    soc.io.ext_intr := 0.U
  } else {
    soc.io.ext_intr := simMMIO.get.io.interrupt.intrVec
    val periCfg = simMMIO.get.cfg.head
    val periDma = simMMIO.get.dma.head
    val socCfg = AxiUtils.getIntnl(cfgMain)
    val socDma = AxiUtils.getIntnl(dmaMain)

    connByName(periCfg.aw, socCfg.aw)
    connByName(periCfg.ar, socCfg.ar)
    connByName(periCfg.w, socCfg.w)
    connByName(socCfg.r, periCfg.r)
    connByName(socCfg.b, periCfg.b)

    connByName(socDma.aw, periDma.aw)
    connByName(socDma.ar, periDma.ar)
    connByName(socDma.w, periDma.w)
    connByName(periDma.r, socDma.r)
    connByName(periDma.b, socDma.b)
  }

  private val l_simAXIMem = LazyModule(new DummyDramMoudle(soc.ddrIO.params))
  private val simAXIMem = Module(l_simAXIMem.module)
  private val memAxi = simAXIMem.axi.head
  private val socDdr = AxiUtils.getIntnl(soc.ddrIO)
  connByName(memAxi.aw, socDdr.aw)
  connByName(memAxi.ar, socDdr.ar)
  connByName(memAxi.w, socDdr.w)
  connByName(socDdr.r, memAxi.r)
  connByName(socDdr.b, memAxi.b)

  val freq = 100
  val cnt = RegInit((freq - 1).U)
  val tick = cnt < (freq / 2).U
  cnt := Mux(cnt === 0.U, (freq - 1).U, cnt - 1.U)

  private val ccns = p(ZJParametersKey).localRing.filter(_.nodeType == NodeType.CC).map(_.attr)
  private val bootAddr = if(ccns.contains("boom")) 0x80000000L.U else 0x10000000L.U
  private val socReset = reset.asAsyncReset.asBool || soc.io.ndreset
  soc.io.rtc_clock := tick
  soc.io.noc_clock := clock
  soc.io.cluster_clocks.foreach(_ := clock)
  soc.io.reset := socReset.asAsyncReset
  soc.io.dft := DontCare
  soc.io.dft.reset.lgc_rst_n := true.B.asAsyncReset
  soc.io.default_reset_vector := bootAddr
  soc.io.chip := 0.U


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
      simMMIO.get.io.uart <> io.uart.get
    })

  }

  if(hasCsu && p(DebugOptionsKey).EnableLuaScoreBoard) {
    val l2StrSeq = soc.ccns.zipWithIndex.map(n => s"[${n._2}] = { ${p(L2ParamKey).nrSlice}, ${n._1.cpuNum} }")
    val l2Str = l2StrSeq.reduce((a, b) => s"$a, $b")
    val nrPcu = p(ZJParametersKey).localRing.count(_.nodeType == NodeType.HF)
    val dcuSeq = p(ZJParametersKey).localRing.filter(n => n.nodeType == NodeType.S && !n.mainMemory)
    val dcuStr = dcuSeq.groupBy(_.bankId).map(ns => {
      val nodeIdStr = ns._2.map(n => s"0x${n.nodeId.toHexString}").reduce((a, b) => s"$a, $b")
      s"[${ns._1}] = {$nodeIdStr}"
    }).reduce((a, b) => s"$a, $b")
    val luaScb = Module(new LuaScoreboard(s"{ $l2Str }", nrPcu, dcuSeq.groupBy(_.bankId).size, s"{ $dcuStr }"))
    luaScb.io.clock := clock
    luaScb.io.reset := reset.asBool
    luaScb.io.sim_final := io.simFinal.get
  }

  if(p(TestIoOptionsKey).removeCsu) {
    for(i <- soc.core.get.indices) {
      val ext = IO(new BlockTestIO(soc.core.get(i).params))
      ext.suggestName(s"core_icn_$i")
      ext <> soc.core.get(i)
    }
  } else if(p(TestIoOptionsKey).removeCore) {
    for(i <- soc.core.get.indices) {
      val noc = soc.core.get(i)
      val hub = LazyModule(new SimCoreHub(noc.params)(p.alterPartial({
        case MonitorsEnabled => false
        case TLUserKey => TLUserParams(aliasBits = p(DcacheKey).aliasBitsOpt.getOrElse(0))
      })))
      val _hub = Module(hub.module)
      val ext = IO(new SimCoreHubExtIO(_hub.io.ext.cio.params, _hub.io.ext.dcache.params, _hub.io.ext.icache.params))
      ext.suggestName(s"core_$i")
      hub.suggestName(s"corehub_$i")
      ext <> _hub.io.ext
      noc <> _hub.io.noc
    }
  }
  private val timer = Wire(UInt(64.W))
  timer := GTimer()
  dontTouch(timer)
  if (!doBlockTest) {
    val logEnable = Wire(Bool())
    val clean = Wire(Bool())
    val dump = Wire(Bool())
    logEnable := (timer >= io.logCtrl.get.log_begin) && (timer < io.logCtrl.get.log_end)
    clean := RegNext(io.perfInfo.get.clean, false.B)
    dump := io.perfInfo.get.dump
    dontTouch(timer)
    dontTouch(logEnable)
    dontTouch(clean)
    dontTouch(dump)
  }

  if(!doBlockTest) DifftestModule.finish("XiangShan")
}

object SimGenerator extends App {
  val (config, firrtlOpts) = SimArgParser(args)
  xs.utils.GlobalData.prefix = config(MiscKey).prefix
  difftest.GlobalData.prefix = config(MiscKey).prefix
  (new ChiselStage).execute(firrtlOpts, Generator.firtoolOpts(config(MiscKey).random) ++ Seq(
    ChiselGeneratorAnnotation(() => {
      DisableMonitors(p => new SimTop()(p))(config)
    })
  ))
  if(config(ZJParametersKey).tfbParams.isDefined) TrafficBoardFileManager.release("generated-src", "generated-src", config)
  FileRegisters.write(filePrefix = config(MiscKey).prefix + "LNTop.")
}
