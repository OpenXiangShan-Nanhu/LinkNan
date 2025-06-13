set project_name "ln_simple"
create_project $project_name $project_name -part xcvu19p-fsva3824-2-e
set_property simulator_language Verilog [current_project]

set ddr_xdc [file join [pwd] "ddr.xdc"]
set ln_xdc  [file join [pwd] "ln.xdc"]
add_files -fileset constrs_1 -norecurse [list $ddr_xdc $ln_xdc]

set ln_fl [open "linknan/FullSys.f" r]
set ln_path [pwd]
append ln_path "/linknan"

set all_files_raw [read $ln_fl]
regsub -all {\$release_path} $all_files_raw $ln_path all_files
set cleaned_list {}
foreach item [split $all_files "\n"] {
  if {$item ne ""} {
    lappend cleaned_list $item
  }
}

add_files -norecurse -scan_for_includes $cleaned_list

set_property file_type Verilog [get_files XlnFpgaTop.sv]

cd [join [list [pwd] $project_name] "/"]

create_bd_design $project_name
create_bd_cell -type module -reference XlnFpgaTop ln
set_property CONFIG.FREQ_HZ 50000000 [get_bd_intf_pins /ln/m_axi_cfg_main]
set_property CONFIG.FREQ_HZ 50000000 [get_bd_intf_pins /ln/m_axi_mem_0]

# start of u_jtag_ddr_subsystem
create_bd_cell -type hier u_jtag_ddr_subsys
create_bd_cell -type ip -vlnv xilinx.com:ip:ddr4 u_jtag_ddr_subsys/ddr4_0
create_bd_cell -type ip -vlnv xilinx.com:ip:jtag_axi u_jtag_ddr_subsys/jtag_axi_0
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_interconnect u_jtag_ddr_subsys/axi_interconnect_0
create_bd_cell -type ip -vlnv xilinx.com:ip:ila u_jtag_ddr_subsys/jtag_maxi_ila
create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset u_jtag_ddr_subsys/rst_ddr4_200M
create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset u_jtag_ddr_subsys/rst_sys_50M
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilvector_logic u_jtag_ddr_subsys/logic_not_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilvector_logic u_jtag_ddr_subsys/logic_not_1
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_clock_converter:2.1 u_jtag_ddr_subsys/axi_clock_converter_0

set_property -dict [list CONFIG.C_DATA_DEPTH {2048} CONFIG.ALL_PROBE_SAME_MU {false}] [get_bd_cells u_jtag_ddr_subsys/jtag_maxi_ila]

set_property -dict [list \
  CONFIG.C_OPERATION {not} \
  CONFIG.C_SIZE {1} \
] [get_bd_cells u_jtag_ddr_subsys/logic_not_0]

set_property -dict [list \
  CONFIG.C_OPERATION {not} \
  CONFIG.C_SIZE {1} \
] [get_bd_cells u_jtag_ddr_subsys/logic_not_1]

set_property -dict [list \
  CONFIG.M_AXI_ID_WIDTH {4} \
  CONFIG.M_HAS_BURST {0} \
  CONFIG.RD_TXN_QUEUE_LENGTH {8} \
  CONFIG.WR_TXN_QUEUE_LENGTH {8} \
] [get_bd_cells u_jtag_ddr_subsys/jtag_axi_0]

set_property -dict [list \
  CONFIG.NUM_SI {2} \
  CONFIG.NUM_MI {1} \
  CONFIG.ENABLE_ADVANCED_OPTIONS {1} \
  CONFIG.M00_HAS_DATA_FIFO {2} \
  CONFIG.M00_HAS_REGSLICE {3} \
  CONFIG.S00_HAS_DATA_FIFO {2} \
  CONFIG.S00_HAS_REGSLICE {4} \
  CONFIG.S01_HAS_DATA_FIFO {2} \
  CONFIG.S01_HAS_REGSLICE {4} \
  CONFIG.STRATEGY {2} \
] [get_bd_cells u_jtag_ddr_subsys/axi_interconnect_0]

set_property -dict [list \
  CONFIG.C0.DDR4_TimePeriod {1250} \
  CONFIG.C0.DDR4_InputClockPeriod {12500} \
  CONFIG.C0.DDR4_CLKOUT0_DIVIDE {6} \
  CONFIG.C0.DDR4_MemoryType {SODIMMs} \
  CONFIG.C0.DDR4_MemoryPart {MTA8ATF1G64HZ-2G3} \
  CONFIG.C0.DDR4_DataWidth {64} \
  CONFIG.C0.DDR4_CasLatency {11} \
  CONFIG.C0.DDR4_CasWriteLatency {11} \
  CONFIG.C0.DDR4_AxiDataWidth {64} \
  CONFIG.C0.DDR4_AxiAddressWidth {33} \
  CONFIG.ADDN_UI_CLKOUT1_FREQ_HZ {50} \
  CONFIG.ADDN_UI_CLKOUT2_FREQ_HZ {25} \
  CONFIG.ADDN_UI_CLKOUT3_FREQ_HZ {10} \
] [get_bd_cells u_jtag_ddr_subsys/ddr4_0]

set_property -dict [list CONFIG.ACLK_ASYNC.VALUE_SRC USER] [get_bd_cells u_jtag_ddr_subsys/axi_clock_converter_0]
set_property CONFIG.ACLK_ASYNC {1} [get_bd_cells u_jtag_ddr_subsys/axi_clock_converter_0]

# u_jtag_ddr_subsys ports
create_bd_pin -dir O u_jtag_ddr_subsys/calib_complete
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/calib_complete] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_init_calib_complete]
create_bd_pin -dir O -from 0 -to 0 u_jtag_ddr_subsys/SOC_RESETN
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_RESETN] [get_bd_pins u_jtag_ddr_subsys/rst_sys_50M/peripheral_aresetn]
create_bd_pin -dir I u_jtag_ddr_subsys/soc_rstn
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/soc_rstn] [get_bd_pins u_jtag_ddr_subsys/logic_not_1/Op1]
create_bd_pin -dir I u_jtag_ddr_subsys/ddr_rstn
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr_rstn] [get_bd_pins u_jtag_ddr_subsys/logic_not_0/Op1]
create_bd_pin -dir O u_jtag_ddr_subsys/SOC_CLK
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_CLK] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/addn_ui_clkout1]
create_bd_pin -dir O u_jtag_ddr_subsys/MAC_CLK
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/MAC_CLK] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/addn_ui_clkout2]
create_bd_pin -dir O u_jtag_ddr_subsys/RTC_CLK
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/RTC_CLK] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/addn_ui_clkout3]
create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 u_jtag_ddr_subsys/S_AXI_MEM
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/S_AXI_MEM] [get_bd_intf_pins u_jtag_ddr_subsys/axi_clock_converter_0/S_AXI]
create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 u_jtag_ddr_subsys/OSC_SYS_CLK
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/OSC_SYS_CLK] [get_bd_intf_pins u_jtag_ddr_subsys/ddr4_0/C0_SYS_CLK]
create_bd_intf_pin -mode Master -vlnv xilinx.com:interface:ddr4_rtl:1.0 u_jtag_ddr_subsys/DDR4
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/DDR4] [get_bd_intf_pins u_jtag_ddr_subsys/ddr4_0/C0_DDR4]

# u_jtag_ddr_subsys connections
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/axi_clock_converter_0/M_AXI] -boundary_type upper [get_bd_intf_pins u_jtag_ddr_subsys/axi_interconnect_0/S01_AXI]
connect_bd_intf_net -boundary_type upper [get_bd_intf_pins u_jtag_ddr_subsys/axi_interconnect_0/M00_AXI] [get_bd_intf_pins u_jtag_ddr_subsys/ddr4_0/C0_DDR4_S_AXI]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/jtag_axi_0/M_AXI] -boundary_type upper [get_bd_intf_pins u_jtag_ddr_subsys/axi_interconnect_0/S00_AXI]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/jtag_maxi_ila/SLOT_0_AXI] [get_bd_intf_pins u_jtag_ddr_subsys/jtag_axi_0/M_AXI]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/logic_not_1/Res] [get_bd_pins u_jtag_ddr_subsys/rst_sys_50M/ext_reset_in]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/logic_not_0/Res] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/sys_rst]

connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk_sync_rst] [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/ext_reset_in]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/slowest_sync_clk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/jtag_maxi_ila/clk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/jtag_axi_0/aclk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/ACLK]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/M00_ACLK]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/S00_ACLK]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/axi_clock_converter_0/m_axi_aclk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_ui_clk] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/S01_ACLK]

connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/M00_ARESETN]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/S00_ARESETN]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/ARESETN]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_interconnect_0/S01_ARESETN]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_clock_converter_0/m_axi_aresetn]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/peripheral_aresetn] [get_bd_pins u_jtag_ddr_subsys/jtag_axi_0/aresetn]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/peripheral_aresetn] [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_ddr4_aresetn]

connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/addn_ui_clkout1] [get_bd_pins u_jtag_ddr_subsys/rst_sys_50M/slowest_sync_clk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/addn_ui_clkout1] [get_bd_pins u_jtag_ddr_subsys/axi_clock_converter_0/s_axi_aclk]

connect_bd_net [get_bd_pins u_jtag_ddr_subsys/rst_sys_50M/interconnect_aresetn] [get_bd_pins u_jtag_ddr_subsys/axi_clock_converter_0/s_axi_aresetn]

connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_init_calib_complete] [get_bd_pins u_jtag_ddr_subsys/rst_sys_50M/dcm_locked]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/ddr4_0/c0_init_calib_complete] [get_bd_pins u_jtag_ddr_subsys/rst_ddr4_200M/dcm_locked]

regenerate_bd_layout -hierarchy [get_bd_cells u_jtag_ddr_subsys]
# end of u_jtag_ddr_subsys

# start of u_peri_subsys
create_bd_cell -type hier u_peri_subsys
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_interconnect u_peri_subsys/axi_interconnect_0
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_uart16550 u_peri_subsys/axi_uart16550_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant u_peri_subsys/xlconstant_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant u_peri_subsys/xlconstant_1

set_property CONFIG.NUM_MI {1} [get_bd_cells u_peri_subsys/axi_interconnect_0]
set_property CONFIG.CONST_VAL {0} [get_bd_cells u_peri_subsys/xlconstant_0]
set_property CONFIG.CONST_VAL {1} [get_bd_cells u_peri_subsys/xlconstant_1]

# u_peri_subsys ports
create_bd_pin -dir I u_peri_subsys/ACLK
create_bd_pin -dir I u_peri_subsys/ARESETN
create_bd_pin -dir O u_peri_subsys/uart0_intc
create_bd_pin -dir O u_peri_subsys/uart0_sout
create_bd_pin -dir I u_peri_subsys/uart0_sin
connect_bd_net [get_bd_pins u_peri_subsys/uart0_intc] [get_bd_pins u_peri_subsys/axi_uart16550_0/ip2intc_irpt]
create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 u_peri_subsys/S_AXI_CFG
connect_bd_intf_net [get_bd_intf_pins u_peri_subsys/S_AXI_CFG] -boundary_type upper [get_bd_intf_pins u_peri_subsys/axi_interconnect_0/S00_AXI]

# u_peri_subsys connections
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/freeze]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/dsrn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/dcdn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_1/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/ctsn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_1/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/rin]
connect_bd_net [get_bd_pins u_peri_subsys/uart0_sin] [get_bd_pins u_peri_subsys/axi_uart16550_0/sin]
connect_bd_net [get_bd_pins u_peri_subsys/uart0_sout] [get_bd_pins u_peri_subsys/axi_uart16550_0/sout]
connect_bd_intf_net -boundary_type upper [get_bd_intf_pins u_peri_subsys/axi_interconnect_0/M00_AXI] [get_bd_intf_pins u_peri_subsys/axi_uart16550_0/S_AXI]

connect_bd_net [get_bd_pins u_peri_subsys/ACLK] [get_bd_pins u_peri_subsys/axi_uart16550_0/s_axi_aclk]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK] [get_bd_pins u_peri_subsys/axi_interconnect_0/M00_ACLK]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK] [get_bd_pins u_peri_subsys/axi_interconnect_0/S00_ACLK]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK] [get_bd_pins u_peri_subsys/axi_interconnect_0/ACLK]

connect_bd_net [get_bd_pins u_peri_subsys/ARESETN] [get_bd_pins u_peri_subsys/axi_uart16550_0/s_axi_aresetn]
connect_bd_net [get_bd_pins u_peri_subsys/ARESETN] [get_bd_pins u_peri_subsys/axi_interconnect_0/S00_ARESETN]
connect_bd_net [get_bd_pins u_peri_subsys/ARESETN] [get_bd_pins u_peri_subsys/axi_interconnect_0/ARESETN]
connect_bd_net [get_bd_pins u_peri_subsys/ARESETN] [get_bd_pins u_peri_subsys/axi_interconnect_0/M00_ARESETN]

regenerate_bd_layout -hierarchy [get_bd_cells u_peri_subsys]
# end of u_peri_subsys

create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant boot_addr
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz clk_wiz_0
create_bd_cell -type ip -vlnv xilinx.com:ip:vio vio_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant intr_lo
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant intr_hi
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconcat intr_cat

create_bd_port -dir O uart0_sout
create_bd_port -dir I uart0_sin
create_bd_port -dir O -type data led3
create_bd_intf_port -mode Master -vlnv xilinx.com:interface:ddr4_rtl:1.0 DDR0
create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 ddr
create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 core

set_property -dict [list \
  CONFIG.CONST_VAL {0x80000000} \
  CONFIG.CONST_WIDTH {48} \
] [get_bd_cells boot_addr]

set_property -dict [list \
  CONFIG.C_NUM_PROBE_IN {0} \
  CONFIG.C_NUM_PROBE_OUT {2} \
] [get_bd_cells vio_0]

set_property -dict [list \
  CONFIG.CONST_VAL {0} \
  CONFIG.CONST_WIDTH {39} \
] [get_bd_cells intr_lo]

set_property -dict [list \
  CONFIG.CONST_VAL {0} \
  CONFIG.CONST_WIDTH {216} \
] [get_bd_cells intr_hi]

set_property -dict [list CONFIG.IN1_WIDTH.VALUE_SRC USER CONFIG.IN2_WIDTH.VALUE_SRC USER CONFIG.IN0_WIDTH.VALUE_SRC USER] [get_bd_cells intr_cat]
set_property -dict [list \
  CONFIG.IN0_WIDTH {39} \
  CONFIG.IN2_WIDTH {216} \
  CONFIG.NUM_PORTS {3} \
] [get_bd_cells intr_cat]

set_property -dict [list \
  CONFIG.CLKIN1_JITTER_PS {200.0} \
  CONFIG.CLKOUT1_JITTER {146.190} \
  CONFIG.CLKOUT1_PHASE_ERROR {154.678} \
  CONFIG.CLKOUT1_REQUESTED_OUT_FREQ {80.000} \
  CONFIG.MMCM_CLKFBOUT_MULT_F {24.000} \
  CONFIG.MMCM_CLKIN1_PERIOD {20.000} \
  CONFIG.MMCM_CLKIN2_PERIOD {10.0} \
  CONFIG.MMCM_CLKOUT0_DIVIDE_F {15.000} \
  CONFIG.PRIM_IN_FREQ {50.000} \
  CONFIG.PRIM_SOURCE {Differential_clock_capable_pin} \
  CONFIG.USE_RESET {false} \
  CONFIG.USE_LOCKED {false} \
] [get_bd_cells clk_wiz_0]

connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/OSC_SYS_CLK] [get_bd_intf_ports ddr]
set_property CONFIG.FREQ_HZ 80000000 [get_bd_intf_ports /ddr]
connect_bd_intf_net [get_bd_intf_pins clk_wiz_0/CLK_IN1_D] [get_bd_intf_ports core]
set_property CONFIG.FREQ_HZ [get_property CONFIG.FREQ_HZ [get_bd_intf_pins clk_wiz_0/CLK_IN1_D]] [get_bd_intf_ports core]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/DDR4] [get_bd_intf_ports DDR0]

connect_bd_net [get_bd_pins boot_addr/dout] [get_bd_pins ln/io_reset_vector]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/RTC_CLK] [get_bd_pins ln/io_rtc_clk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_CLK] [get_bd_pins ln/io_aclk]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_RESETN] [get_bd_pins ln/io_aresetn]
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_RESETN] [get_bd_pins u_peri_subsys/ARESETN] -boundary_type upper
connect_bd_net [get_bd_pins u_jtag_ddr_subsys/SOC_CLK] [get_bd_pins u_peri_subsys/ACLK] -boundary_type upper
connect_bd_net [get_bd_pins clk_wiz_0/clk_out1] [get_bd_pins vio_0/clk]
connect_bd_net [get_bd_pins clk_wiz_0/clk_out1] [get_bd_pins ln/io_core_clk_0]
connect_bd_intf_net [get_bd_intf_pins ln/m_axi_cfg_main] -boundary_type upper [get_bd_intf_pins u_peri_subsys/S_AXI_CFG]
connect_bd_intf_net [get_bd_intf_pins ln/m_axi_mem_0] -boundary_type upper [get_bd_intf_pins u_jtag_ddr_subsys/S_AXI_MEM]
connect_bd_net [get_bd_ports uart0_sin] [get_bd_pins u_peri_subsys/uart0_sin]
connect_bd_net [get_bd_ports uart0_sout] [get_bd_pins u_peri_subsys/uart0_sout]
connect_bd_net [get_bd_ports led3] [get_bd_pins u_jtag_ddr_subsys/calib_complete]
connect_bd_net [get_bd_pins vio_0/probe_out0] [get_bd_pins u_jtag_ddr_subsys/soc_rstn]
connect_bd_net [get_bd_pins vio_0/probe_out1] [get_bd_pins u_jtag_ddr_subsys/ddr_rstn]
connect_bd_net [get_bd_pins intr_lo/dout] [get_bd_pins intr_cat/In0]
connect_bd_net [get_bd_pins intr_hi/dout] [get_bd_pins intr_cat/In2]
connect_bd_net [get_bd_pins u_peri_subsys/uart0_intc] [get_bd_pins intr_cat/In1]
connect_bd_net [get_bd_pins ln/io_ext_intr] [get_bd_pins intr_cat/dout]

regenerate_bd_layout

assign_bd_address -target_address_space /ln/m_axi_cfg_main [get_bd_addr_segs u_peri_subsys/axi_uart16550_0/S_AXI/Reg] -force
set_property offset 0x310B0000 [get_bd_addr_segs {ln/m_axi_cfg_main/SEG_axi_uart16550_0_Reg}]
assign_bd_address -target_address_space /ln/m_axi_mem_0 [get_bd_addr_segs u_jtag_ddr_subsys/ddr4_0/C0_DDR4_MEMORY_MAP/C0_DDR4_ADDRESS_BLOCK] -force
assign_bd_address -target_address_space /u_jtag_ddr_subsys/jtag_axi_0/Data [get_bd_addr_segs u_jtag_ddr_subsys/ddr4_0/C0_DDR4_MEMORY_MAP/C0_DDR4_ADDRESS_BLOCK] -force

validate_bd_design
save_bd_design
close_bd_design [get_bd_designs $project_name]

set wrp_name $project_name
append wrp_name "_wrapper"
set bd_file [file join [pwd] "$project_name.srcs" "sources_1" "bd" $project_name "$project_name.bd"]
set bd_wrp_file [file join [pwd] "$project_name.gen" "sources_1" "bd" $project_name "hdl" "$wrp_name.v"]
make_wrapper -files [get_files $bd_file] -top
add_files -norecurse $bd_wrp_file
set_property top $wrp_name [current_fileset]
update_compile_order -fileset sources_1
generate_target all [get_files $bd_file]
export_ip_user_files -of_objects [get_files $bd_file] -no_script -sync -force -quiet
create_ip_run [get_files -of_objects [get_fileset sources_1] $bd_file]
launch_runs synth_1 -jobs 8