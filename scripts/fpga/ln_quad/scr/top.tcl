create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant boot_addr
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant ddr_offset
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard in_mmcm
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard core_0_pll
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard core_1_pll
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard core_2_pll
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard core_3_pll
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wizard noc_pll
create_bd_cell -type ip -vlnv xilinx.com:ip:axis_vio vio_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant intr_lo
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant intr_hi
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconcat intr_cat
create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset noc_reset_gen

create_bd_port -dir O uart0_sout
create_bd_port -dir I uart0_sin
create_bd_intf_port -mode Master -vlnv xilinx.com:interface:ddr4_rtl:1.0 DDR0
create_bd_intf_port -mode Slave -vlnv xilinx.com:interface:diff_clock_rtl:1.0 sys

set_property -dict [list \
  CONFIG.CONST_VAL {0x80000000} \
  CONFIG.CONST_WIDTH {48} \
] [get_bd_cells boot_addr]

set_property -dict [list \
  CONFIG.CONST_VAL {0x800000000} \
  CONFIG.CONST_WIDTH {48} \
] [get_bd_cells ddr_offset]

set_property -dict [list \
  CONFIG.C_NUM_PROBE_IN {1} \
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
  CONFIG.C_EN_PROBE_IN_ACTIVITY {0} \
  CONFIG.C_NUM_PROBE_IN {0} \
  CONFIG.C_NUM_PROBE_OUT {1} \
] [get_bd_cells vio_0]

set_property -dict [list CONFIG.PRIM_IN_FREQ.VALUE_SRC USER] [get_bd_cells in_mmcm]
set_property -dict [list \
  CONFIG.CLKOUT_DRIVES {BUFG,BUFG,BUFGCE,BUFGCE,BUFGCE,BUFGCE,BUFGCE} \
  CONFIG.CLKOUT_DYN_PS {None,None,None,None,None,None,None} \
  CONFIG.CLKOUT_GROUPING {Auto,Auto,Auto,Auto,Auto,Auto,Auto} \
  CONFIG.CLKOUT_MATCHED_ROUTING {false,false,false,false,false,false,false} \
  CONFIG.CLKOUT_PORT {sys_clk,rtc_clk,clk_out3,clk_out4,clk_out5,clk_out6,clk_out7} \
  CONFIG.CLKOUT_REQUESTED_DUTY_CYCLE {50.000,50.000,50.000,50.000,50.000,50.000,50.000} \
  CONFIG.CLKOUT_REQUESTED_OUT_FREQUENCY {100.000,10.000,100.000,100.000,100.000,100.000,100.000} \
  CONFIG.CLKOUT_REQUESTED_PHASE {0.000,0.000,0.000,0.000,0.000,0.000,0.000} \
  CONFIG.CLKOUT_USED {true,true,false,false,false,false,false} \
  CONFIG.PRIM_IN_FREQ {50.000} \
  CONFIG.PRIM_SOURCE {Differential_clock_capable_pin} \
  CONFIG.USE_LOCKED {false} \
  CONFIG.USE_RESET {false} \
  CONFIG.USE_SAFE_CLOCK_STARTUP {false} \
] [get_bd_cells in_mmcm]

set_property -dict [list \
  CONFIG.CLKOUT_DRIVES {BUFG,BUFG,BUFG,BUFG,BUFG,BUFG,BUFG} \
  CONFIG.CLKOUT_DYN_PS {None,None,None,None,None,None,None} \
  CONFIG.CLKOUT_GROUPING {Auto,Auto,Auto,Auto,Auto,Auto,Auto} \
  CONFIG.CLKOUT_MATCHED_ROUTING {false,false,false,false,false,false,false} \
  CONFIG.CLKOUT_PORT {core_clk,clk_out2,clk_out3,clk_out4,clk_out5,clk_out6,clk_out7} \
  CONFIG.CLKOUT_REQUESTED_DUTY_CYCLE {50.000,50.000,50.000,50.000,50.000,50.000,50.000} \
  CONFIG.CLKOUT_REQUESTED_OUT_FREQUENCY {100.000,100.000,100.000,100.000,100.000,100.000,100.000} \
  CONFIG.CLKOUT_REQUESTED_PHASE {0.000,0.000,0.000,0.000,0.000,0.000,0.000} \
  CONFIG.CLKOUT_USED {true,false,false,false,false,false,false} \
  CONFIG.PRIMITIVE_TYPE {PLL} \
  CONFIG.PRIM_SOURCE {Global_buffer} \
] [get_bd_cells core_*_pll]

set_property -dict [list \
  CONFIG.CLKOUT_DRIVES {BUFG,BUFG,BUFG,BUFG,BUFG,BUFG,BUFG} \
  CONFIG.CLKOUT_DYN_PS {None,None,None,None,None,None,None} \
  CONFIG.CLKOUT_GROUPING {Auto,Auto,Auto,Auto,Auto,Auto,Auto} \
  CONFIG.CLKOUT_MATCHED_ROUTING {false,false,false,false,false,false,false} \
  CONFIG.CLKOUT_PORT {noc_clk,peri_clk,clk_out3,clk_out4,clk_out5,clk_out6,clk_out7} \
  CONFIG.CLKOUT_REQUESTED_DUTY_CYCLE {50.000,50.000,50.000,50.000,50.000,50.000,50.000} \
  CONFIG.CLKOUT_REQUESTED_OUT_FREQUENCY {100.000,50,100.000,100.000,100.000,100.000,100.000} \
  CONFIG.CLKOUT_REQUESTED_PHASE {0.000,0.000,0.000,0.000,0.000,0.000,0.000} \
  CONFIG.CLKOUT_USED {true,true,false,false,false,false,false} \
  CONFIG.PRIMITIVE_TYPE {PLL} \
  CONFIG.PRIM_SOURCE {Global_buffer} \
  CONFIG.USE_LOCKED {true} \
] [get_bd_cells noc_pll]

connect_bd_intf_net [get_bd_intf_pins in_mmcm/CLK_IN1_D] [get_bd_intf_ports sys]
set_property CONFIG.FREQ_HZ [get_property CONFIG.FREQ_HZ [get_bd_intf_pins in_mmcm/CLK_IN1_D]] [get_bd_intf_ports sys]
connect_bd_intf_net [get_bd_intf_pins u_jtag_ddr_subsys/DDR4] [get_bd_intf_ports DDR0]
set_property CONFIG.FREQ_HZ [get_property CONFIG.FREQ_HZ  [get_bd_pins noc_pll/noc_clk]] [get_bd_intf_pins ln/m_axi_cfg]
set_property CONFIG.CLK_DOMAIN [get_property CONFIG.CLK_DOMAIN [get_bd_pins noc_pll/noc_clk]] [get_bd_intf_pins ln/m_axi_cfg]
set_property CONFIG.FREQ_HZ [get_property CONFIG.FREQ_HZ  [get_bd_pins noc_pll/noc_clk]]  [get_bd_intf_pins ln/m_axi_mem_0]
set_property CONFIG.CLK_DOMAIN [get_property CONFIG.CLK_DOMAIN [get_bd_pins noc_pll/noc_clk]] [get_bd_intf_pins ln/m_axi_mem_0]
set_property CONFIG.FREQ_HZ [get_property CONFIG.FREQ_HZ  [get_bd_pins noc_pll/noc_clk]] [get_bd_pins ln/io_aclk]

connect_bd_intf_net [get_bd_intf_ports sys] [get_bd_intf_pins in_mmcm/CLK_IN1_D]
connect_bd_net [get_bd_pins in_mmcm/sys_clk] [get_bd_pins core_*_pll/clk_in1]
connect_bd_net [get_bd_pins in_mmcm/sys_clk] [get_bd_pins noc_pll/clk_in1]
connect_bd_net [get_bd_pins in_mmcm/sys_clk] [get_bd_pins u_jtag_ddr_subsys/DDR_CLK]
connect_bd_net [get_bd_pins in_mmcm/rtc_clk] [get_bd_pins ln/io_rtc_clk]
connect_bd_net [get_bd_pins core_0_pll/core_clk] [get_bd_pins ln/io_core_clk_0]
connect_bd_net [get_bd_pins core_1_pll/core_clk] [get_bd_pins ln/io_core_clk_1]
connect_bd_net [get_bd_pins core_2_pll/core_clk] [get_bd_pins ln/io_core_clk_2]
connect_bd_net [get_bd_pins core_3_pll/core_clk] [get_bd_pins ln/io_core_clk_3]
connect_bd_net [get_bd_pins noc_pll/noc_clk] [get_bd_pins vio_0/clk]
connect_bd_net [get_bd_pins noc_pll/noc_clk] [get_bd_pins ln/io_aclk]
connect_bd_net [get_bd_pins noc_pll/noc_clk] [get_bd_pins u_peri_subsys/ACLK_NOC]
connect_bd_net [get_bd_pins noc_pll/noc_clk] [get_bd_pins u_jtag_ddr_subsys/S_AXI_MEM_ACLK]
connect_bd_net [get_bd_pins noc_pll/peri_clk] [get_bd_pins u_peri_subsys/ACLK_PERI]

connect_bd_net [get_bd_pins noc_pll/noc_clk] [get_bd_pins noc_reset_gen/slowest_sync_clk]
connect_bd_net [get_bd_pins noc_pll/locked] [get_bd_pins noc_reset_gen/dcm_locked]
connect_bd_net [get_bd_pins vio_0/probe_out0] [get_bd_pins noc_reset_gen/ext_reset_in]
connect_bd_net [get_bd_pins noc_reset_gen/peripheral_aresetn] [get_bd_pins u_peri_subsys/ARESETN]
connect_bd_net [get_bd_pins noc_reset_gen/interconnect_aresetn] [get_bd_pins ln/io_aresetn]

connect_bd_net [get_bd_pins boot_addr/dout] [get_bd_pins ln/io_reset_vector]
connect_bd_net [get_bd_pins ddr_offset/dout] [get_bd_pins ln/io_ddr_offset]
connect_bd_intf_net [get_bd_intf_pins ln/m_axi_cfg] -boundary_type upper [get_bd_intf_pins u_peri_subsys/S_AXI_CFG]
connect_bd_intf_net [get_bd_intf_pins ln/m_axi_mem_0] -boundary_type upper [get_bd_intf_pins u_jtag_ddr_subsys/S_AXI_MEM]
connect_bd_net [get_bd_ports uart0_sin] [get_bd_pins u_peri_subsys/uart0_sin]
connect_bd_net [get_bd_ports uart0_sout] [get_bd_pins u_peri_subsys/uart0_sout]
connect_bd_net [get_bd_pins intr_lo/dout] [get_bd_pins intr_cat/In0]
connect_bd_net [get_bd_pins intr_hi/dout] [get_bd_pins intr_cat/In2]
connect_bd_net [get_bd_pins u_peri_subsys/uart0_intc] [get_bd_pins intr_cat/In1]
connect_bd_net [get_bd_pins ln/io_ext_intr] [get_bd_pins intr_cat/dout]

regenerate_bd_layout

assign_bd_address
set_property offset 0x310B0000 [get_bd_addr_segs {ln/m_axi_cfg/SEG_axi_uart16550_0_Reg}]
