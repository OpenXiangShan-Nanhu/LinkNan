create_bd_cell -type hier u_jtag_ddr_subsys
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_noc u_jtag_ddr_subsys/axi_noc
create_bd_cell -type ip -vlnv xilinx.com:ip:ps_wizard u_jtag_ddr_subsys/ps

set_property -dict [list \
  CONFIG.PS_PMC_CONFIG(PMC_USE_PMC_AXI_NOC0) {1} \
  CONFIG.PS_PMC_CONFIG(PS_SLR_ID) {0} \
] [get_bd_cells u_jtag_ddr_subsys/ps]

set_property -dict [list \
  CONFIG.MC_CHAN_REGION0 {DDR_LOW1} \
  CONFIG.MC_INPUTCLK0_PERIOD {10000} \
  CONFIG.MC_MEMORY_DEVICETYPE {SODIMMs} \
  CONFIG.MC_MEMORY_SPEEDGRADE {DDR4-2400T(17-17-17)} \
  CONFIG.MC_MEMORY_TIMEPERIOD0 {1250} \
  CONFIG.MC_PRE_DEF_ADDR_MAP_SEL {ROW_COLUMN_BANK} \
  CONFIG.MC_RANK {2} \
  CONFIG.MC_ROWADDRESSWIDTH {17} \
  CONFIG.MC_SYSTEM_CLOCK {No_Buffer} \
  CONFIG.NUM_CLKS {2} \
  CONFIG.NUM_MC {1} \
  CONFIG.NUM_MI {0} \
  CONFIG.NUM_SI {2} \
  CONFIG.SI_SIDEBAND_PINS {} \
] [get_bd_cells u_jtag_ddr_subsys/axi_noc]

set_property -dict [list \
  CONFIG.CONNECTIONS {MC_0 {read_bw {3200} write_bw {3200} read_avg_burst {4} write_avg_burst {4}}} \
  CONFIG.R_TRAFFIC_CLASS {LOW_LATENCY} \
  CONFIG.W_TRAFFIC_CLASS {BEST_EFFORT}
] [get_bd_intf_pins /u_jtag_ddr_subsys/axi_noc/S00_AXI]

set_property -dict [list \
  CONFIG.CATEGORY {ps_pmc} \
  CONFIG.CONNECTIONS {MC_0 {read_bw {500} write_bw {500} read_avg_burst {4} write_avg_burst {4}}} \
] [get_bd_intf_pins /u_jtag_ddr_subsys/axi_noc/S01_AXI]

# u_jtag_ddr_subsys ports
create_bd_pin -dir I u_jtag_ddr_subsys/S_AXI_MEM_ACLK
create_bd_pin -dir I u_jtag_ddr_subsys/DDR_CLK
create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 u_jtag_ddr_subsys/S_AXI_MEM
create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:ddr4_rtl:1.0 u_jtag_ddr_subsys/DDR4

# u_jtag_ddr_subsys connections
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/DDR_CLK] [get_bd_pins u_jtag_ddr_subsys/axi_noc/sys_clk0]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/S_AXI_MEM] [get_bd_intf_pins u_jtag_ddr_subsys/axi_noc/S00_AXI]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/S_AXI_MEM_ACLK] [get_bd_pins u_jtag_ddr_subsys/axi_noc/aclk0]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/ps/PMC_AXI_NOC0] [get_bd_intf_pins u_jtag_ddr_subsys/axi_noc/S01_AXI]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ps/pmc_axi_noc0_clk] [get_bd_pins u_jtag_ddr_subsys/axi_noc/aclk1]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/DDR4] [get_bd_intf_pins u_jtag_ddr_subsys/axi_noc/CH0_DDR4_0]

# Place NoC Components
set_property -dict [list CONFIG.PHYSICAL_LOC {NOC_NMU512_S0X3Y11}] [get_bd_intf_pins /u_jtag_ddr_subsys/axi_noc/S00_AXI]
set_property -dict [list CONFIG.PHYSICAL_LOC {DDRMC_S0X3Y0}] [get_bd_intf_pins /u_jtag_ddr_subsys/axi_noc/CH0_DDR4_0]