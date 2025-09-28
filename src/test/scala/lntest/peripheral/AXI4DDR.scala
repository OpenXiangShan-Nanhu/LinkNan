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

package lntest.peripheral

import chisel3._
import chisel3.util._
import chisel3.experimental.attach
import org.chipsalliance.cde.config.Parameters
import org.chipsalliance.diplomacy.lazymodule._
import freechips.rocketchip.amba.axi4.AXI4SlaveNode
import freechips.rocketchip.diplomacy.AddressSet
import lntest.top._

class AXI4DDR
(
  address: Seq[AddressSet],
  executable: Boolean = true,
  beatBytes: Int = 8,
  burstLen: Int = 16,
  dll_off   : Boolean = true,
  ddr4_mode : Int     = 0,        // 0-dll_off, 1-2400P, 2-3200W
  dbi_en    : Boolean = false,
  rank_num  : Int     = 2,
)(implicit p: Parameters)
  extends AXI4SlaveModule(address, executable, beatBytes, burstLen)
{
  override lazy val module = new AXI4SlaveModuleImp(this) {
    val ddr4_3200w: Boolean = ddr4_mode == 2
    val ddr4_2400p: Boolean = ddr4_mode == 1
    val ddr4_lowpower: Boolean = ddr4_mode == 0
    require(ddr4_mode == 0 || ddr4_mode == 1 || ddr4_mode == 2, "ddr4_mode should be 0, 1 or 2")

    val FREQ_RATIO = 2
    val MC_REG_MSTR_DLL_MODE      = if(dll_off) 1.U(1.W) else 0.U(1.W)
    val MC_REG_REFRESH_MARGIN     = 2.U(4.W)
    val MC_REG_REFRESH_TO_X1_X32  = 16.U(5.W)
    val MC_REG_REFRESH_BURST      = 7.U(6.W)
    val MC_REG_PER_BANK_REFRESH   = 0.U(1.W)
    val MC_REG_REFRESH_TIMER1_START_VALUE_x32 = 0.U(13.W)
    val MC_REG_REFRESH_TIMER0_START_VALUE_x32 = 0.U(13.W)
    val MEM_CLK_PERIOD = if(ddr4_3200w) 0.625 else if(ddr4_2400p) 0.834 else 10
    val MC_REG_T_RFC_NOM_X1_X32 = (((7800 / MEM_CLK_PERIOD).floor / FREQ_RATIO).floor / 32).floor.toInt.U(12.W)
    val MC_REG_T_RFC_MIN = ((350 / MEM_CLK_PERIOD).ceil / FREQ_RATIO).ceil.toInt.U(10.W)
    val MC_REG_INIT0        = if(ddr4_3200w) "h0001_0187".U(32.W) else "h0001_001f".U(32.W)
    val MC_REG_INIT3_MR     = if(ddr4_3200w) "h0c44".U(16.W)  else if(ddr4_2400p) "h0070".U(16.W) else "h0004".U(16.W)
    val MC_REG_INIT3_EMR    = if(ddr4_3200w || ddr4_2400p) "h0001".U(16.W) else "h0000".U(16.W)
    val MC_REG_INIT4_EMR2   = if(ddr4_3200w) "h0028".U(16.W) else if(ddr4_2400p) "h0018".U(16.W) else "h0000".U(16.W)
    val MC_REG_INIT4_EMR3   = if(ddr4_3200w) "h0400".U(16.W) else "h0000".U(16.W)
    val MC_REG_INIT7_MR6    = if(ddr4_3200w) "h1000".U(16.W) else "h0000".U(16.W)
    val MC_REG_INIT6_MR5_DBI  = if(dbi_en) "h1840".U(16.W) else "h0400".U(16.W)
    val MC_REG_DBI_CTL        = if(dbi_en) "h0000_0006".U(32.W) else "h0000_0000".U(32.W)
    val MC_REG_DIMMCTL_DIMM_MRS_BG1_EN    = if(rank_num > 1) 1.U(1.W) else 0.U(1.W)
    val MC_REG_DIMMCTL_DIMM_ADDR_MIRR_EN  = if(rank_num > 1) 1.U(1.W) else 0.U(1.W)
    val MC_REG_DIMMCTL_DIMMSTAGGER_CS_EN  = if(rank_num > 1) 1.U(1.W) else 0.U(1.W)
    val MC_REG_DRAMTMG0   = if(ddr4_3200w) "h1611_361a".U(32.W) else if(ddr4_2400p) "h1212_2814".U(32.W) else "h0e05_0604".U(32.W)
    val MC_REG_DRAMTMG1   = if(ddr4_3200w) "h0005_0624".U(32.W) else if(ddr4_2400p) "h0004_051b".U(32.W) else "h0008_0c02".U(32.W)
    val MC_REG_DRAMTMG2   = if(ddr4_3200w) "h080a_0510".U(32.W) else if(ddr4_2400p) "h0608_0613".U(32.W) else "h0909_0613".U(32.W)
    val MC_REG_DRAMTMG3   = if(ddr4_3200w) "h0000_400c".U(32.W) else if(ddr4_2400p) "h0000_400a".U(32.W) else "h0000_8018".U(32.W)
    val MC_REG_DRAMTMG4   = if(ddr4_3200w) "h0a04_040b".U(32.W) else if(ddr4_2400p) "h0803_0309".U(32.W) else "h0105_0402".U(32.W)
    val MC_REG_DRAMTMG5   = if(ddr4_3200w) "h0808_0504".U(32.W) else if(ddr4_2400p) "h0707_0504".U(32.W) else "h0055_0403".U(32.W)
    val MC_REG_DRAMTMG8   = if(ddr4_3200w) "h0505_1009".U(32.W) else if(ddr4_2400p) "h0606_0f06".U(32.W) else "h0101_2012".U(32.W)
    val MC_REG_DRAMTMG9   = if(ddr4_3200w) "h0002_020c".U(32.W) else if(ddr4_2400p) "h0002_020a".U(32.W) else "h0004_040f".U(32.W)
    val MC_REG_DRAMTMG10  = if(ddr4_3200w) "h000e_0c05".U(32.W) else if(ddr4_2400p) "h000e_0b05".U(32.W) else "h001c_180a".U(32.W)
    val MC_REG_DRAMTMG11  = if(ddr4_3200w) "h190b_010e".U(32.W) else if(ddr4_2400p) "h1409_010e".U(32.W) else "h2011_011c".U(32.W)
    val MC_REG_DRAMTMG12  = if(ddr4_3200w) "h0c00_0008".U(32.W) else if(ddr4_2400p) "h0c00_0008".U(32.W) else "h1800_0012".U(32.W)
    val MC_REG_DRAMTMG15  = if(ddr4_3200w) "h0000_0000".U(32.W) else if(ddr4_2400p) "h8000_0000".U(32.W) else "h8000_0000".U(32.W)

    val ddrClockResetGen = Module(new DDRClockResetGen)
    val mc    = Module(new DWC_ddr_umctl2)
    val phy   = Module(new dfiphy_ddr4)
    val ddr4  = Module(new jedec_ddr4_16GB_2r_64_udimm)

    io.init_done := ddrClockResetGen.io.mc_init_done
    dontTouch(io.init_done)
    
    ddrClockResetGen.io.sys_rst_n := true.B

    mc.io   := DontCare   // TODO
    phy.io  := DontCare   // TODO
    ddr4.io := DontCare   // TODO

    mc.io.core_ddrc_rstn  := ddrClockResetGen.io.mc_rst_n
    mc.io.aresetn_0       := ddrClockResetGen.io.axi_rst_n
    mc.io.presetn         := ddrClockResetGen.io.apb_rst_n

    mc.io.core_ddrc_core_clk  := ddrClockResetGen.io.mc_clk
    mc.io.pclk                := ddrClockResetGen.io.apb_clk
    phy.io.DFI_CLK            := ddrClockResetGen.io.mc_clk
    phy.io.PHY_CLK            := ddrClockResetGen.io.phy_clk

    mc.io.aclk_0 := clock
    mc.io.csysreq_ddrc := true.B
    mc.io.csysreq_0 := true.B
    mc.io.awvalid_0 := in.aw.valid
    in.aw.ready := mc.io.awready_0
    mc.io.awid_0 := in.aw.bits.id
    mc.io.awaddr_0 := in.aw.bits.addr - address.head.base.U
    mc.io.awlen_0 := in.aw.bits.len
    mc.io.awsize_0 := in.aw.bits.size
    mc.io.awburst_0 := in.aw.bits.burst
    mc.io.awlock_0 := in.aw.bits.lock
    mc.io.awcache_0 := in.aw.bits.cache
    mc.io.awprot_0 := in.aw.bits.prot
    mc.io.awqos_0 := in.aw.bits.qos
    // urgen\poison_intr\autopre\region

    mc.io.wvalid_0 := in.w.valid
    in.w.ready := mc.io.wready_0
    mc.io.wdata_0 := in.w.bits.data
    mc.io.wstrb_0 := in.w.bits.strb
    mc.io.wlast_0 := in.w.bits.last

    in.b.valid := mc.io.bvalid_0
    mc.io.bready_0 := in.b.ready
    in.b.bits.id := mc.io.bid_0
    in.b.bits.resp := mc.io.bresp_0

    mc.io.arvalid_0 := in.ar.valid
    in.ar.ready := mc.io.arready_0
    mc.io.arid_0 := in.ar.bits.id
    mc.io.araddr_0 := in.ar.bits.addr - address.head.base.U
    mc.io.arlen_0 := in.ar.bits.len
    mc.io.arsize_0 := in.ar.bits.size
    mc.io.arburst_0 := in.ar.bits.burst
    mc.io.arlock_0 := in.ar.bits.lock
    mc.io.arcache_0 := in.ar.bits.cache
    mc.io.arprot_0 := in.ar.bits.prot
    mc.io.arqos_0 := in.ar.bits.qos

    in.r.valid := mc.io.rvalid_0
    mc.io.rready_0 := in.r.ready
    in.r.bits.id := mc.io.rid_0
    in.r.bits.data := mc.io.rdata_0
    in.r.bits.resp := mc.io.rresp_0
    in.r.bits.last := mc.io.rlast_0

    phy.io.dfi.connect_mc(mc.io.dfi)

    ddr4.io.RESET_N := phy.io.MEM.RESET_N
    attach(ddr4.io.DQ, phy.io.MEM.DQ)
    attach(ddr4.io.DQS, phy.io.MEM.DQS)
    attach(ddr4.io.DQS_N, phy.io.MEM.DQS_N)
    attach(ddr4.io.DM_N, phy.io.MEM.DM)
    ddr4.io.CK      := phy.io.MEM.CK
    ddr4.io.CKE     := phy.io.MEM.CKE(0)
    ddr4.io.CKE1    := phy.io.MEM.CKE(1)
    ddr4.io.CS0_N   := phy.io.MEM.CS(0)
    ddr4.io.CS1_N   := phy.io.MEM.CS(1)
    ddr4.io.ACT_N   := phy.io.MEM.ACT_N
    ddr4.io.RAS_N   := phy.io.MEM.RAS_N
    ddr4.io.CAS_N   := phy.io.MEM.CAS_N
    ddr4.io.WE_N    := phy.io.MEM.WE_N
    ddr4.io.BG      := phy.io.MEM.BG
    ddr4.io.BA      := phy.io.MEM.BA
    ddr4.io.ADDR    := phy.io.MEM.ADDR
    ddr4.io.PAR     := phy.io.MEM.PAR
    phy.io.MEM.ALERT_N := ddr4.io.ALERT_N

    val apb_wr = true.B
    val apb_rd = false.B
    val apb_init_seq = Seq(
      Seq(apb_wr, "h304".U(12.W), "h0000_0001".U(32.W)),                                                  // DBG1
      Seq(apb_wr, "h030".U(12.W), "h0000_0001".U(32.W)),                                                  // PWRCTL
      Seq(apb_wr, "h000".U(12.W), "h4304".U(16.W) ## MC_REG_MSTR_DLL_MODE ## "h10".U(15.W)),              // MSTR
      Seq(apb_wr, "h010".U(12.W), "h0000_f030".U(32.W)),                                                  // MRCTRL1
      Seq(apb_wr, "h014".U(12.W), "h0000_691f".U(32.W)),                                                  // MRCTRL2
      Seq(apb_wr, "h01c".U(12.W), "hd3f0_0d4a".U(32.W)),                                                  // MRCTRL2
      Seq(apb_wr, "h030".U(12.W), "h0000_000a".U(32.W)),                                                  // PWRCTL
      Seq(apb_wr, "h034".U(12.W), "h0005_0003".U(32.W)),                                                  // PWRTMG
      Seq(apb_wr, "h038".U(12.W), "h0035_0000".U(32.W)),                                                  // HWLPCTL
      Seq(apb_wr, "h050".U(12.W), "h0021_0070".U(32.W)),                                                  // RFSHCTL0
      Seq(apb_wr, "h054".U(12.W), "h0000_0000".U(32.W)),                                                  // RFSHCTL1
      Seq(apb_wr, "h060".U(12.W), "h0000_0000".U(32.W)),                                                  // RFSHCTL3
      Seq(apb_wr, "h064".U(12.W), 0.U(4.W) ## MC_REG_T_RFC_NOM_X1_X32 ## 0.U(6.W) ## MC_REG_T_RFC_MIN),   // RFSHTMG
      Seq(apb_wr, "h0c0".U(12.W), "h0000_0000".U(32.W)),                                                  // CRCPARCTL0
      Seq(apb_wr, "h0c4".U(12.W), "h0800_0000".U(32.W)),                                                  // CRCPARCTL1
      Seq(apb_wr, "h0d0".U(12.W), MC_REG_INIT0),                                                          // INIT0
      Seq(apb_wr, "h0d4".U(12.W), "h000d_0000".U(32.W)),                                                  // INIT1
      Seq(apb_wr, "h0dc".U(12.W), MC_REG_INIT3_MR ## MC_REG_INIT3_EMR),                                   // INIT3
      Seq(apb_wr, "h0e0".U(12.W), MC_REG_INIT4_EMR2 ## MC_REG_INIT4_EMR3),                                // INIT4
      Seq(apb_wr, "h0e4".U(12.W), "h0020_0000".U(32.W)),                                                  // INIT5
      Seq(apb_wr, "h0e8".U(12.W), 0.U(16.W) ## MC_REG_INIT6_MR5_DBI),                                     // INIT6
      Seq(apb_wr, "h0ec".U(12.W), 0.U(16.W) ## MC_REG_INIT7_MR6),                                         // INIT7
      Seq(apb_wr, "h0f0".U(12.W), 0.U(27.W) ## MC_REG_DIMMCTL_DIMM_MRS_BG1_EN ##
        0.U(2.W) ## MC_REG_DIMMCTL_DIMM_ADDR_MIRR_EN ## MC_REG_DIMMCTL_DIMMSTAGGER_CS_EN),                // DIMMCTL
      Seq(apb_wr, "h0f4".U(12.W), "h0000_e56b".U(32.W)),                                                  // RANKCTL
      Seq(apb_wr, "h100".U(12.W), MC_REG_DRAMTMG0),                                                       // DRAMTMG0
      Seq(apb_wr, "h104".U(12.W), MC_REG_DRAMTMG1),                                                       // DRAMTMG1
      Seq(apb_wr, "h108".U(12.W), MC_REG_DRAMTMG2),                                                       // DRAMTMG2
      Seq(apb_wr, "h10c".U(12.W), MC_REG_DRAMTMG3),                                                       // DRAMTMG3
      Seq(apb_wr, "h110".U(12.W), MC_REG_DRAMTMG4),                                                       // DRAMTMG4
      Seq(apb_wr, "h114".U(12.W), MC_REG_DRAMTMG5),                                                       // DRAMTMG5
      Seq(apb_wr, "h120".U(12.W), MC_REG_DRAMTMG8),                                                       // DRAMTMG8
      Seq(apb_wr, "h124".U(12.W), MC_REG_DRAMTMG9),                                                       // DRAMTMG9
      Seq(apb_wr, "h128".U(12.W), MC_REG_DRAMTMG10),                                                      // DRAMTMG10
      Seq(apb_wr, "h12c".U(12.W), MC_REG_DRAMTMG11),                                                      // DRAMTMG11
      Seq(apb_wr, "h130".U(12.W), MC_REG_DRAMTMG12),                                                      // DRAMTMG12
      Seq(apb_wr, "h13c".U(12.W), MC_REG_DRAMTMG15),                                                      // DRAMTMG15
      Seq(apb_wr, "h180".U(12.W), "hd100_0040".U(32.W)),                                                  // ZQCRL0
      Seq(apb_wr, "h184".U(12.W), "h0000_0070".U(32.W)),                                                  // ZQCRL1
      Seq(apb_wr, "h190".U(12.W), "h028a_8207".U(32.W)),                                                  // DFITMG0
      Seq(apb_wr, "h194".U(12.W), "h0006_0404".U(32.W)),                                                  // DFITMG1
      Seq(apb_wr, "h198".U(12.W), "h0100_8000".U(32.W)),                                                  // DFILPCFG0
      Seq(apb_wr, "h19c".U(12.W), "h0000_0010".U(32.W)),                                                  // DFILPCFG1
      Seq(apb_wr, "h1a0".U(12.W), "h2040_0002".U(32.W)),                                                  // DFIUPD0
      Seq(apb_wr, "h1a4".U(12.W), "h000f_0080".U(32.W)),                                                  // DFIUPD1
      Seq(apb_wr, "h1a8".U(12.W), "h8000_0000".U(32.W)),                                                  // DFIUPD2
      Seq(apb_wr, "h1b0".U(12.W), "h0000_0061".U(32.W)),                                                  // DFIMISC
      Seq(apb_wr, "h1b4".U(12.W), "h0000_0a09".U(32.W)),                                                  // DFITMG2
      Seq(apb_wr, "h1b8".U(12.W), "h0000_0001".U(32.W)),                                                  // DFITMG3
      Seq(apb_wr, "h1c0".U(12.W), MC_REG_DBI_CTL),                                                        // DBICTL
      Seq(apb_wr, "h1c4".U(12.W), "h8000_0000".U(32.W)),                                                  // DFIPHYMSTR
      Seq(apb_wr, "h200".U(12.W), "h001f_1f18".U(32.W)),                                                  // ADDRMAP0
      Seq(apb_wr, "h204".U(12.W), "h003f_0a0a".U(32.W)),                                                  // ADDRMAP1
      Seq(apb_wr, "h208".U(12.W), "h0202_0200".U(32.W)),                                                  // ADDRMAP2
      Seq(apb_wr, "h20c".U(12.W), "h0202_0202".U(32.W)),                                                  // ADDRMAP3
      Seq(apb_wr, "h210".U(12.W), "h0000_1f1f".U(32.W)),                                                  // ADDRMAP4
      Seq(apb_wr, "h214".U(12.W), "h0808_0808".U(32.W)),                                                  // ADDRMAP5
      Seq(apb_wr, "h218".U(12.W), "h0808_0808".U(32.W)),                                                  // ADDRMAP6
      Seq(apb_wr, "h21c".U(12.W), "h0000_0f0f".U(32.W)),                                                  // ADDRMAP7
      Seq(apb_wr, "h220".U(12.W), "h0000_0101".U(32.W)),                                                  // ADDRMAP8
      Seq(apb_wr, "h224".U(12.W), "h0808_0808".U(32.W)),                                                  // ADDRMAP9
      Seq(apb_wr, "h228".U(12.W), "h0808_0808".U(32.W)),                                                  // ADDRMAP10
      Seq(apb_wr, "h22c".U(12.W), "h001f_1f08".U(32.W)),                                                  // ADDRMAP11
      Seq(apb_wr, "h240".U(12.W), "h0805_0810".U(32.W)),                                                  // ODTCFG
      Seq(apb_wr, "h244".U(12.W), "h0000_1230".U(32.W)),                                                  // ODTMAP
      Seq(apb_wr, "h250".U(12.W), "h0000_0484".U(32.W)),                                                  // SCHED
      Seq(apb_wr, "h254".U(12.W), "h3221_2010".U(32.W)),                                                  // SCHED1
      Seq(apb_wr, "h25c".U(12.W), "hc300_0060".U(32.W)),                                                  // PERFHPR1
      Seq(apb_wr, "h264".U(12.W), "h0a00_0060".U(32.W)),                                                  // PERFLPR1
      Seq(apb_wr, "h26c".U(12.W), "h1000_0060".U(32.W)),                                                  // PERFWR1
      Seq(apb_wr, "h300".U(12.W), "h0000_0001".U(32.W)),                                                  // DBG0
      Seq(apb_wr, "h30c".U(12.W), "h0000_0000".U(32.W)),                                                  // DBGCMD
      Seq(apb_wr, "h320".U(12.W), "h0000_0001".U(32.W)),                                                  // SWCTL
      Seq(apb_wr, "h328".U(12.W), "h0000_0000".U(32.W)),                                                  // SWCTLSTATIC
      Seq(apb_wr, "h36c".U(12.W), "h0011_0011".U(32.W)),                                                  // POISONCFG
      Seq(apb_wr, "h400".U(12.W), "h0000_0000".U(32.W)),                                                  // PCCFG
      Seq(apb_wr, "h404".U(12.W), "h0000_5126".U(32.W)),                                                  // PCFGR_0
      Seq(apb_wr, "h408".U(12.W), "h0000_0029".U(32.W)),                                                  // PCFGW_0
      Seq(apb_wr, "h494".U(12.W), "h0022_000e".U(32.W)),                                                  // PCFGQOS0_0
      Seq(apb_rd, "h490".U(12.W), "h0000_0000".U(32.W)),                                                  // PCTRL_0
      // POST
      Seq(apb_wr, "h304".U(12.W), "h0000_0000".U(32.W)),                                                  // DBG1
      Seq(apb_wr, "h030".U(12.W), "h0000_0080".U(32.W)),                                                  // PWRCTL
      Seq(apb_wr, "h320".U(12.W), "h0000_0000".U(32.W)),                                                  // SWCTL
      Seq(apb_wr, "h1b0".U(12.W), "h0000_0061".U(32.W)),                                                  // DFIMISC
      Seq(apb_rd, "h1bc".U(12.W), "h0000_0001".U(32.W)),                                                  // DFISTAT
      Seq(apb_rd, "h004".U(12.W), "h0000_0001".U(32.W)),                                                  // STAT
      Seq(apb_wr, "h304".U(12.W), "h0000_0000".U(32.W)),                                                  // DBG_2
      Seq(apb_wr, "h490".U(12.W), "h0000_0001".U(32.W)),                                                  // PCTRL_0
    )

    withClockAndReset(ddrClockResetGen.io.apb_clk, ~ddrClockResetGen.io.apb_rst_n) {
      val apb_init_seq_wr   = RegInit(VecInit(apb_init_seq.map(_(0).asBool)))
      val apb_init_seq_addr = RegInit(VecInit(apb_init_seq.map(_(1))))
      val apb_init_seq_data = RegInit(VecInit(apb_init_seq.map(_(2))))
      dontTouch(apb_init_seq_wr)
      dontTouch(apb_init_seq_addr)
      dontTouch(apb_init_seq_data)

      val s_idle :: s_setup :: s_access :: Nil = Enum(3)
      val state = RegInit(s_idle)

      val table_idx = RegInit(0.U(log2Ceil(apb_init_seq.length).W))
      val access_done = Mux(apb_init_seq_wr(table_idx) === apb_wr, mc.io.pready && state === s_access, mc.io.pready && state === s_access && mc.io.prdata === apb_init_seq_data(table_idx))
      when(access_done) {
        table_idx := table_idx + 1.U
      }

      ddrClockResetGen.io.mc_init_stage1_done := table_idx >= (apb_init_seq.length - 8).U
      ddrClockResetGen.io.mc_init_stage2_done := table_idx === apb_init_seq.length.U
      phy.io.RST_N          := table_idx >= (apb_init_seq.length - 8).U
      switch(state) {
        is(s_idle) {
          when(table_idx < apb_init_seq.length.U) {
            state := s_setup
          }
          mc.io.psel := false.B
          mc.io.penable := false.B
          mc.io.pwdata := 0.U
        }
        is(s_setup) {
          state := s_access
          mc.io.penable := false.B
          mc.io.psel := true.B
          mc.io.paddr := apb_init_seq_addr(table_idx)
          mc.io.pwdata := apb_init_seq_data(table_idx)
          mc.io.pwrite := apb_init_seq_wr(table_idx) === apb_wr
        }
        is(s_access) {
          mc.io.penable := true.B
          when(mc.io.pready && table_idx === apb_init_seq.length.U - 1.U) {
            state := s_idle
          }.elsewhen(mc.io.pready) {
            state := s_setup
          }.otherwise {
            state := s_access
          }
        }
      }
    }
  }
}

class AXI4DDRWrapper (
  slave: AXI4SlaveNode,
  memByte: Long,
  useBlackBox: Boolean = false
)(implicit p: Parameters) extends AXI4MemorySlave(slave, memByte, useBlackBox) {
  val ram = LazyModule(new AXI4DDR(
    slaveParam.address, slaveParam.executable, portParam.beatBytes, burstLen
  ))
  ram.node := master

  class Impl extends LazyModuleImp(this) {
    val mc_init_done = IO(Output(Bool()))
    dontTouch(mc_init_done)
    mc_init_done := ram.module.io.init_done
  }
  override lazy val module = new Impl
}
