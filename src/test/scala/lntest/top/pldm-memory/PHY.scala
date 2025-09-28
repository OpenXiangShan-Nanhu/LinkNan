package lntest.top

import chisel3._
import chisel3.experimental.Analog

class DDRPHY_DFIBundle(
  DFI_CS_WIDTH: Int = 2,
  DFI_ADDR_BITS: Int = 36,
  DFI_BANK_ADDR_WIDTH: Int = 3,
  MEM_DATA_BITS: Int = 64,
  MEM_DATA_BYTE_WIDTH: Int = 8,
  DFI_BG_ADDR_WIDTH: Int = 2,
  DFI_CID_WIDTH: Int = 2
) extends Bundle {
  val MEM_NUM_BYTES: Int = (MEM_DATA_BITS + (MEM_DATA_BYTE_WIDTH - 1)) / MEM_DATA_BYTE_WIDTH
  val DFI_DATA_BITS: Int = MEM_DATA_BITS * 2
  val DFI_NUM_BYTES: Int = MEM_NUM_BYTES * 2

  val address_p0 = Input(UInt(DFI_ADDR_BITS.W))
  val address_p1 = Input(UInt(DFI_ADDR_BITS.W))
  val address_p2 = Input(UInt(DFI_ADDR_BITS.W))
  val address_p3 = Input(UInt(DFI_ADDR_BITS.W))

  val bank_p0 = Input(UInt(DFI_BANK_ADDR_WIDTH.W))
  val bank_p1 = Input(UInt(DFI_BANK_ADDR_WIDTH.W))
  val bank_p2 = Input(UInt(DFI_BANK_ADDR_WIDTH.W))
  val bank_p3 = Input(UInt(DFI_BANK_ADDR_WIDTH.W))

  val ras_n_p0 = Input(Bool())
  val ras_n_p1 = Input(Bool())
  val ras_n_p2 = Input(Bool())
  val ras_n_p3 = Input(Bool())

  val cas_n_p0 = Input(Bool())
  val cas_n_p1 = Input(Bool())
  val cas_n_p2 = Input(Bool())
  val cas_n_p3 = Input(Bool())

  val we_n_p0 = Input(Bool())
  val we_n_p1 = Input(Bool())
  val we_n_p2 = Input(Bool())
  val we_n_p3 = Input(Bool())

  val cs_p0 = Input(UInt(DFI_CS_WIDTH.W))
  val cs_p1 = Input(UInt(DFI_CS_WIDTH.W))
  val cs_p2 = Input(UInt(DFI_CS_WIDTH.W))
  val cs_p3 = Input(UInt(DFI_CS_WIDTH.W))

  val act_n_p0 = Input(Bool())
  val act_n_p1 = Input(Bool())
  val act_n_p2 = Input(Bool())
  val act_n_p3 = Input(Bool())

  val bg_p0 = Input(UInt(DFI_BG_ADDR_WIDTH.W))
  val bg_p1 = Input(UInt(DFI_BG_ADDR_WIDTH.W))
  val bg_p2 = Input(UInt(DFI_BG_ADDR_WIDTH.W))
  val bg_p3 = Input(UInt(DFI_BG_ADDR_WIDTH.W))

  val cid_p0 = Input(UInt(DFI_CID_WIDTH.W))
  val cid_p1 = Input(UInt(DFI_CID_WIDTH.W))
  val cid_p2 = Input(UInt(DFI_CID_WIDTH.W))
  val cid_p3 = Input(UInt(DFI_CID_WIDTH.W))

  val cke_p0 = Input(UInt(DFI_CS_WIDTH.W))
  val cke_p1 = Input(UInt(DFI_CS_WIDTH.W))
  val cke_p2 = Input(UInt(DFI_CS_WIDTH.W))
  val cke_p3 = Input(UInt(DFI_CS_WIDTH.W))

  val odt_p0 = Input(UInt(DFI_CS_WIDTH.W))
  val odt_p1 = Input(UInt(DFI_CS_WIDTH.W))
  val odt_p2 = Input(UInt(DFI_CS_WIDTH.W))
  val odt_p3 = Input(UInt(DFI_CS_WIDTH.W))

  val reset_n_p0 = Input(UInt(DFI_CS_WIDTH.W))
  val reset_n_p1 = Input(UInt(DFI_CS_WIDTH.W))
  val reset_n_p2 = Input(UInt(DFI_CS_WIDTH.W))
  val reset_n_p3 = Input(UInt(DFI_CS_WIDTH.W))

  val wrdata_en_p0 = Input(Bool())
  val wrdata_en_p1 = Input(Bool())
  val wrdata_en_p2 = Input(Bool())
  val wrdata_en_p3 = Input(Bool())

  val wrdata_p0 = Input(UInt(DFI_DATA_BITS.W))
  val wrdata_p1 = Input(UInt(DFI_DATA_BITS.W))
  val wrdata_p2 = Input(UInt(DFI_DATA_BITS.W))
  val wrdata_p3 = Input(UInt(DFI_DATA_BITS.W))

  val wrdata_cs_n_p0 = Input(UInt((DFI_CS_WIDTH).W))
  val wrdata_cs_n_p1 = Input(UInt((DFI_CS_WIDTH).W))
  val wrdata_cs_n_p2 = Input(UInt((DFI_CS_WIDTH).W))
  val wrdata_cs_n_p3 = Input(UInt((DFI_CS_WIDTH).W))

  val wrdata_mask_p0 = Input(UInt(DFI_NUM_BYTES.W))
  val wrdata_mask_p1 = Input(UInt(DFI_NUM_BYTES.W))
  val wrdata_mask_p2 = Input(UInt(DFI_NUM_BYTES.W))
  val wrdata_mask_p3 = Input(UInt(DFI_NUM_BYTES.W))

  val rddata_en_p0 = Input(Bool())
  val rddata_en_p1 = Input(Bool())
  val rddata_en_p2 = Input(Bool())
  val rddata_en_p3 = Input(Bool())

  val rddata_w0 = Output(UInt(DFI_DATA_BITS.W))
  val rddata_w1 = Output(UInt(DFI_DATA_BITS.W))
  val rddata_w2 = Output(UInt(DFI_DATA_BITS.W))
  val rddata_w3 = Output(UInt(DFI_DATA_BITS.W))
  
  val rddata_cs_n_p0 = Input(UInt(DFI_CS_WIDTH.W))
  val rddata_cs_n_p1 = Input(UInt(DFI_CS_WIDTH.W))
  val rddata_cs_n_p2 = Input(UInt(DFI_CS_WIDTH.W))
  val rddata_cs_n_p3 = Input(UInt(DFI_CS_WIDTH.W))

  val rddata_valid_w0 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_valid_w1 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_valid_w2 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_valid_w3 = Output(UInt(DFI_NUM_BYTES.W))

  val rddata_dbi_w0 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_dbi_w1 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_dbi_w2 = Output(UInt(DFI_NUM_BYTES.W))
  val rddata_dbi_w3 = Output(UInt(DFI_NUM_BYTES.W))

  val ctrlupd_req = Input(Bool())
  val ctrlupd_ack = Output(Bool())

  val dram_clk_disable = Input(Bool())
  val freq_ratio = Input(UInt(2.W))
  val init_complete = Output(Bool())
  val init_start = Input(Bool())
  val parity_in_p0 = Input(Bool())
  val parity_in_p1 = Input(Bool())
  val parity_in_p2 = Input(Bool())
  val parity_in_p3 = Input(Bool())
  val alert_n_a0 = Output(Bool())
  val alert_n_a1 = Output(Bool())
  val alert_n_a2 = Output(Bool())
  val alert_n_a3 = Output(Bool())

  val lp_ctrl_req = Input(Bool())
  val lp_data_req = Input(Bool())
  val lp_wakeup = Input(UInt(4.W))
  val lp_ack = Output(Bool())

  def connect_mc(mc_io: MC_DFIBundle) = {
    address_p0 := mc_io.address(mc_io.address.getWidth/2 - 1, 0)
    address_p1 := mc_io.address(mc_io.address.getWidth - 1, mc_io.address.getWidth/2)
    address_p2 := DontCare
    address_p3 := DontCare

    bank_p0 := mc_io.bank(mc_io.bank.getWidth/2 - 1, 0)
    bank_p1 := mc_io.bank(mc_io.bank.getWidth - 1, mc_io.bank.getWidth/2)
    bank_p2 := 0.U
    bank_p3 := 0.U

    ras_n_p0 := mc_io.ras_n(0).asBool
    ras_n_p1 := mc_io.ras_n(1).asBool
    ras_n_p2 := true.B
    ras_n_p3 := true.B

    cas_n_p0 := mc_io.cas_n(0).asBool
    cas_n_p1 := mc_io.cas_n(1).asBool
    cas_n_p2 := true.B
    cas_n_p3 := true.B

    we_n_p0 := mc_io.we_n(0).asBool
    we_n_p1 := mc_io.we_n(1).asBool
    we_n_p2 := true.B
    we_n_p3 := true.B

    cs_p0 := mc_io.cs(mc_io.cs.getWidth/2 - 1, 0)
    cs_p1 := mc_io.cs(mc_io.cs.getWidth - 1, mc_io.cs.getWidth/2)
    cs_p2 := 1.U
    cs_p3 := 1.U

    act_n_p0 := mc_io.act_n(0).asBool
    act_n_p1 := mc_io.act_n(1).asBool
    act_n_p2 := true.B
    act_n_p3 := true.B

    bg_p0 := mc_io.bg(mc_io.bg.getWidth/2 - 1, 0)
    bg_p1 := mc_io.bg(mc_io.bg.getWidth - 1, mc_io.bg.getWidth/2)
    bg_p2 := DontCare
    bg_p3 := DontCare

    cke_p0 := mc_io.cke(mc_io.cke.getWidth/2 - 1, 0)
    cke_p1 := mc_io.cke(mc_io.cke.getWidth - 1, mc_io.cke.getWidth/2)
    cke_p2 := DontCare
    cke_p3 := DontCare

    reset_n_p0 := mc_io.reset_n(mc_io.reset_n.getWidth/2 - 1, 0)
    reset_n_p1 := mc_io.reset_n(mc_io.reset_n.getWidth - 1, mc_io.reset_n.getWidth/2)
    reset_n_p2 := 1.U
    reset_n_p3 := 1.U

    wrdata_en_p0 := mc_io.wrdata_en(0).asBool
    wrdata_en_p1 := mc_io.wrdata_en(mc_io.wrdata_en.getWidth / 2).asBool
    wrdata_en_p2 := DontCare
    wrdata_en_p3 := DontCare
    
    wrdata_p0 := mc_io.wrdata(mc_io.wrdata.getWidth/2 - 1, 0)
    wrdata_p1 := mc_io.wrdata(mc_io.wrdata.getWidth - 1, mc_io.wrdata.getWidth/2)
    wrdata_p2 := DontCare
    wrdata_p3 := DontCare

    wrdata_cs_n_p0 := mc_io.wrdata_cs(mc_io.wrdata_cs.getWidth/2 - 1, 0)
    wrdata_cs_n_p1 := mc_io.wrdata_cs(mc_io.wrdata_cs.getWidth - 1, mc_io.wrdata_cs.getWidth/2)
    wrdata_cs_n_p2 := DontCare
    wrdata_cs_n_p3 := DontCare

    wrdata_mask_p0 := mc_io.wrdata_mask(mc_io.wrdata_mask.getWidth/2 - 1, 0)
    wrdata_mask_p1 := mc_io.wrdata_mask(mc_io.wrdata_mask.getWidth - 1, mc_io.wrdata_mask.getWidth/2)
    wrdata_mask_p2 := DontCare
    wrdata_mask_p3 := DontCare

    rddata_en_p0 := mc_io.rddata_en(0).asBool
    rddata_en_p1 := mc_io.rddata_en(mc_io.rddata_en.getWidth / 2).asBool
    rddata_en_p2 := DontCare
    rddata_en_p3 := DontCare

    mc_io.rddata := rddata_w1 ## rddata_w0

    rddata_cs_n_p0 := mc_io.rddata_cs(mc_io.rddata_cs.getWidth/2 - 1, 0)
    rddata_cs_n_p1 := mc_io.rddata_cs(mc_io.rddata_cs.getWidth - 1, mc_io.rddata_cs.getWidth/2)
    rddata_cs_n_p2 := DontCare
    rddata_cs_n_p3 := DontCare

    mc_io.rddata_valid := rddata_valid_w1(mc_io.rddata_valid.getWidth/2 - 1, 0) ## rddata_valid_w0(mc_io.rddata_valid.getWidth/2 - 1, 0)
    mc_io.rddata_dbi := rddata_dbi_w1(mc_io.rddata_dbi.getWidth/2 - 1, 0) ## rddata_dbi_w0(mc_io.rddata_dbi.getWidth/2 - 1, 0)

    ctrlupd_req := mc_io.ctrlupd_req
    mc_io.ctrlupd_ack := ctrlupd_ack
    dram_clk_disable := mc_io.dram_clk_disable
    freq_ratio := 1.U
    init_start := mc_io.init_start
    mc_io.init_complete := init_complete
    parity_in_p0 := mc_io.parity_in(0).asBool
    parity_in_p1 := mc_io.parity_in(1).asBool
    parity_in_p2 := DontCare
    parity_in_p3 := DontCare

    mc_io.alert_n := alert_n_a1 ## alert_n_a0
  }
}

class DDRPHY_DRAMBundle(
  DFI_CS_WIDTH: Int = 2,
  DFI_ADDR_BITS: Int = 36,
  DFI_BANK_ADDR_WIDTH: Int = 3,
  MEM_DATA_BITS: Int = 64,
  MEM_DATA_BYTE_WIDTH: Int = 8,
  DFI_BG_ADDR_WIDTH: Int = 2,
  DFI_CID_WIDTH: Int = 2
) extends Bundle {
  val MEM_NUM_BYTES: Int = (MEM_DATA_BITS + (MEM_DATA_BYTE_WIDTH - 1)) / MEM_DATA_BYTE_WIDTH
  val DFI_DATA_BITS: Int = MEM_DATA_BITS * 2
  val DFI_NUM_BYTES: Int = MEM_NUM_BYTES * 2

  val DQ      = Analog(MEM_DATA_BITS.W)
  val DQS     = Analog(MEM_NUM_BYTES.W)
  val DQS_N   = Analog(MEM_NUM_BYTES.W)
  val CK      = Output(Bool())
  val CK_N    = Output(Bool())
  val CKE     = Output(UInt(DFI_CS_WIDTH.W))
  val CS      = Output(UInt(DFI_CS_WIDTH.W))
  val PAR     = Output(Bool())
  val RESET_N = Output(UInt(DFI_CS_WIDTH.W))
  val ODT     = Output(UInt(DFI_CS_WIDTH.W))
  val RAS_N   = Output(Bool())
  val BA      = Output(UInt(DFI_BANK_ADDR_WIDTH.W))
  val CAS_N   = Output(Bool())
  val WE_N    = Output(Bool())
  val ADDR    = Output(UInt(DFI_ADDR_BITS.W))
  val DM      = Analog(MEM_NUM_BYTES.W)
  val BG      = Output(UInt(DFI_BG_ADDR_WIDTH.W))
  val CID     = Output(UInt(DFI_CID_WIDTH.W))
  val ACT_N   = Output(Bool())
  val ALERT_N = Input(Bool())
}

class dfiphy_ddr4(
  DFI_CS_WIDTH: Int = 2,
  DFI_ADDR_BITS: Int = 36,
  DFI_BANK_ADDR_WIDTH: Int = 3,
  MEM_DATA_BITS: Int = 64,
  MEM_DATA_BYTE_WIDTH: Int = 8,
  DFI_BG_ADDR_WIDTH: Int = 2,
  DFI_CID_WIDTH: Int = 2
) extends BlackBox(Map(
  "dfi_cs_width" -> DFI_CS_WIDTH,
  "dfi_addr_bits" -> DFI_ADDR_BITS,
  "dfi_bank_addr_width" -> DFI_BANK_ADDR_WIDTH,
  "mem_data_bits" -> MEM_DATA_BITS,
  "mem_data_byte_width" -> MEM_DATA_BYTE_WIDTH,
  "t_wrlat_adj" -> 0,
  "t_rddata_en_adj" -> 0
)) {
  val MEM_NUM_BYTES: Int = (MEM_DATA_BITS + (MEM_DATA_BYTE_WIDTH - 1)) / MEM_DATA_BYTE_WIDTH
  val DFI_DATA_BITS: Int = MEM_DATA_BITS * 2
  val DFI_NUM_BYTES: Int = MEM_NUM_BYTES * 2

  val io = IO(new Bundle {
    val DFI_CLK = Input(Clock())
    val PHY_CLK = Input(Clock())
    val RST_N = Input(Reset())
    val dfi = new DDRPHY_DFIBundle
    val MEM = new DDRPHY_DRAMBundle
  })
}