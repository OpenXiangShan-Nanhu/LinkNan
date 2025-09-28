package lntest.top

import chisel3._

class MC_DFIBundle(
  FREQ_RATIO: Int = 2,
  DFI_ADDR_WIDTH: Int = 18,
  BANK_BITS: Int = 3,
  BG_BITS: Int = 2,
  NUM_RANKS: Int = 2,
  RESET_WIDTH: Int = 2,
  DFI_TOTAL_DATA_WIDTH: Int = 256,
  DFI_TOTAL_MASK_WIDTH: Int = 32,
  DFI_TOTAL_DATAEN_WIDTH: Int = 16
) extends Bundle {
  val act_n = Output(UInt(FREQ_RATIO.W))
  val address = Output(UInt((FREQ_RATIO * DFI_ADDR_WIDTH).W))
  val bank = Output(UInt((FREQ_RATIO * BANK_BITS).W))
  val freq_ratio = Output(UInt(2.W))
  val bg = Output(UInt((FREQ_RATIO * BG_BITS).W))
  val cas_n = Output(UInt(FREQ_RATIO.W))
  val ras_n = Output(UInt(FREQ_RATIO.W))
  val we_n = Output(UInt(FREQ_RATIO.W))
  val cke = Output(UInt((FREQ_RATIO * NUM_RANKS).W))
  val cs = Output(UInt((FREQ_RATIO * NUM_RANKS).W))
  val odt = Output(UInt((FREQ_RATIO * NUM_RANKS).W))
  val reset_n = Output(UInt((FREQ_RATIO * RESET_WIDTH).W))
  val wrdata = Output(UInt(DFI_TOTAL_DATA_WIDTH.W))
  val wrdata_mask = Output(UInt(DFI_TOTAL_MASK_WIDTH.W))
  val wrdata_en = Output(UInt(DFI_TOTAL_DATAEN_WIDTH.W))
  val rddata = Input(UInt(DFI_TOTAL_DATA_WIDTH.W))
  val rddata_en = Output(UInt(DFI_TOTAL_DATAEN_WIDTH.W))
  val rddata_valid = Input(UInt(DFI_TOTAL_DATAEN_WIDTH.W))
  val rddata_dbi = Input(UInt((DFI_TOTAL_DATA_WIDTH / 8).W))
  val wrdata_cs = Output(UInt((NUM_RANKS * DFI_TOTAL_DATAEN_WIDTH).W))
  val rddata_cs = Output(UInt((NUM_RANKS * DFI_TOTAL_DATAEN_WIDTH).W))
  val ctrlupd_ack = Input(Bool())
  val ctrlupd_ack2 = Input(Bool())
  val ctrlupd_req = Output(Bool())
  val dram_clk_disable = Output(Bool())
  val parity_in = Output(UInt(FREQ_RATIO.W))
  val alert_n = Input(UInt(FREQ_RATIO.W))
  val init_start = Output(Bool())
  val init_complete = Input(Bool())
  val frequency = Output(UInt(5.W))
  val geardown_en = Output(Bool())
  val lp_req = Output(Bool())
  val lp_ack = Input(Bool())
  val phyupd_req = Input(Bool())
  val phyupd_ack = Output(Bool())
  val phyupd_type = Input(UInt(3.W))
  val lp_wakeup = Output(UInt(6.W))
  val phymstr_req = Input(Bool())
  val phymstr_cs_state = Input(UInt(NUM_RANKS.W))
  val phymstr_state_sel = Input(Bool())
  val phymstr_type = Input(UInt(2.W))
  val phymstr_ack = Output(Bool())
  val alert_err_intr = Output(Bool())
}

class DWC_ddr_umctl2(
  ID_WIDTH: Int = 5,
  ADDR_WIDTH: Int = 37,
  LEN_WIDTH: Int = 8,
  LOCK_WIDTH: Int = 1,
  DATA_WIDTH: Int = 256,
  RESP_WIDTH: Int = 2,
  SIZE_WIDTH: Int = 3,
  CACHE_WIDTH: Int = 4,
  PROT_WIDTH: Int = 3,
  BURST_WIDTH: Int = 2,
  QOS_WIDTH: Int = 4,
  REGION_WIDTH: Int = 4,
  NPORTS: Int = 1,
  APB_AW: Int = 12,
  APB_DW: Int = 32
) extends BlackBox {
  val io = IO(new Bundle {
    val core_ddrc_core_clk = Input(Clock())
    val core_ddrc_rstn = Input(Reset())

    val ctl_idle = Output(Bool())
    // AXI
    val aresetn_0 = Input(Reset())
    val aclk_0 = Input(Clock())
    // aw
    val awid_0 = Input(UInt(ID_WIDTH.W))
    val awaddr_0 = Input(UInt(ADDR_WIDTH.W))
    val awlen_0 = Input(UInt(LEN_WIDTH.W))
    val awsize_0 = Input(UInt(SIZE_WIDTH.W))
    val awburst_0 = Input(UInt(BURST_WIDTH.W))
    val awlock_0 = Input(UInt(LOCK_WIDTH.W))
    val awcache_0 = Input(UInt(CACHE_WIDTH.W))
    val awprot_0 = Input(UInt(PROT_WIDTH.W))
    val awvalid_0 = Input(Bool())
    val awready_0 = Output(Bool())
    val awqos_0 = Input(UInt(QOS_WIDTH.W))
    // val awurgent_0 = Input(Bool())
    // val awpoison_0 = Input(Bool())
    // val awpoison_intr_0 = Output(Bool())
    // val awautopre_0 = Input(Bool())
    // val awregion_0 = Input(UInt(REGION_WIDTH.W))
    // w
    val wdata_0 = Input(UInt(DATA_WIDTH.W))
    val wstrb_0 = Input(UInt((DATA_WIDTH / 8).W))
    val wlast_0 = Input(Bool())
    val wvalid_0 = Input(Bool())
    val wready_0 = Output(Bool())
    // b
    val bid_0 = Output(UInt(ID_WIDTH.W))
    val bresp_0 = Output(UInt(RESP_WIDTH.W))
    val bvalid_0 = Output(Bool())
    val bready_0 = Input(Bool())
    // ar
    val arid_0 = Input(UInt(ID_WIDTH.W))
    val araddr_0 = Input(UInt(ADDR_WIDTH.W))
    val arlen_0 = Input(UInt(LEN_WIDTH.W))
    val arsize_0 = Input(UInt(SIZE_WIDTH.W))
    val arburst_0 = Input(UInt(BURST_WIDTH.W))
    val arlock_0 = Input(UInt(LOCK_WIDTH.W))
    val arcache_0 = Input(UInt(CACHE_WIDTH.W))
    val arprot_0 = Input(UInt(PROT_WIDTH.W))
    val arvalid_0 = Input(Bool())
    val arready_0 = Output(Bool())
    val arqos_0 = Input(UInt(QOS_WIDTH.W))
    // val arpoison_0 = Input(Bool())
    // val arpoison_intr_0 = Output(Bool())
    // val arautopre_0 = Input(Bool())
    // val arregion_0 = Input(UInt(REGION_WIDTH.W))
    // val arurgent_0 = Input(Bool())
    // r
    val rid_0 = Output(UInt(ID_WIDTH.W))
    val rdata_0 = Output(UInt(DATA_WIDTH.W))
    val rresp_0 = Output(UInt(RESP_WIDTH.W))
    val rlast_0 = Output(Bool())
    val rvalid_0 = Output(Bool())
    val rready_0 = Input(Bool())

    val csysreq_0 = Input(Bool())
    val csysack_0 = Output(Bool())
    val cactive_0 = Output(Bool())

    // val raq_wcount_0 = Output(UInt(XPI_RAQD_LG2_0.W))
    // val raq_pop_0 = Output(Bool())
    // val raq_push_0 = Output(Bool())
    // val raq_split_0 = Output(Bool())
    // val waq_wcount_0 = Output(UInt(XPI_WAQD_LG2_0.W))
    // val waq_pop_0 = Output(Bool())
    // val waq_push_0 = Output(Bool())
    // val waq_split_0 = Output(Bool())

    // val hif_mrr_data = Output(UInt(MRR_DATA_TOTAL_DATA_WIDTH.W))
    // val hif_mrr_data_valid = Output(Bool())

    val csysreq_ddrc = Input(Bool())
    val csysack_ddrc = Output(Bool())
    val cactive_ddrc = Output(Bool())
    
    val stat_ddrc_reg_selfref_type = Output(UInt(2.W))

    val lpr_credit_cnt = Output(UInt(7.W))
    val hpr_credit_cnt = Output(UInt(7.W))
    val wr_credit_cnt = Output(UInt(7.W))

    val pa_rmask = Input(UInt((NPORTS * 2).W))
    val pa_wmask = Input(UInt(NPORTS.W))

    val scanmode = Input(Bool())
    val scan_resetn = Input(Bool())

    // apb
    val pclk = Input(Clock())
    val presetn = Input(Reset())
    val paddr = Input(UInt(APB_AW.W))
    val pwdata = Input(UInt(APB_DW.W))
    val pwrite = Input(Bool())
    val psel = Input(Bool())
    val penable = Input(Bool())
    val pready = Output(Bool())
    val prdata = Output(UInt(APB_DW.W))
    val pslverr = Output(Bool())

    val dfi = new MC_DFIBundle
  })
}