
create_bd_cell -type hier u_peri_subsys
create_bd_cell -type ip -vlnv xilinx.com:ip:smartconnect u_peri_subsys/axi_interconnect_0
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_uart16550 u_peri_subsys/axi_uart16550_0
create_bd_cell -type ip -vlnv xilinx.com:ip:axi_bram_ctrl u_peri_subsys/axi_bram_ctrl_0
create_bd_cell -type ip -vlnv xilinx.com:ip:emb_mem_gen u_peri_subsys/boot_mem
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant u_peri_subsys/xlconstant_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant u_peri_subsys/xlconstant_1
create_bd_cell -type ip -vlnv xilinx.com:ip:proc_sys_reset u_peri_subsys/reset_sync

set_property CONFIG.NUM_MI {1} [get_bd_cells u_peri_subsys/axi_interconnect_0]
set_property CONFIG.CONST_VAL {0} [get_bd_cells u_peri_subsys/xlconstant_0]
set_property CONFIG.CONST_VAL {1} [get_bd_cells u_peri_subsys/xlconstant_1]

set_property -dict [list \
  CONFIG.NUM_CLKS {2} \
  CONFIG.NUM_SI {1} \
  CONFIG.NUM_MI {2} \
] [get_bd_cells u_peri_subsys/axi_interconnect_0]

set_property -dict [list \
  CONFIG.ALGORITHM {Minimum_Area} \
  CONFIG.ENABLE_BYTE_WRITES_A {true} \
  CONFIG.MEMORY_DEPTH {1024} \
  CONFIG.WRITE_DATA_WIDTH_A {32} \
  CONFIG.MEMORY_TYPE {Single_Port_RAM} \
  CONFIG.USE_MEMORY_BLOCK {Stand_Alone} \
  CONFIG.READ_LATENCY_A {2} \
] [get_bd_cells u_peri_subsys/boot_mem]
set ln_rom [file join [pwd] "constr" "boot.mem"]
set_property CONFIG.MEMORY_INIT_FILE $ln_rom [get_bd_cells u_peri_subsys/boot_mem]

set_property -dict [list \
  CONFIG.DATA_WIDTH {32} \
  CONFIG.PROTOCOL {AXI4} \
  CONFIG.SINGLE_PORT_BRAM {1} \
  CONFIG.READ_LATENCY {2} \
  CONFIG.USE_ECC {0} \
] [get_bd_cells u_peri_subsys/axi_bram_ctrl_0]

create_bd_pin -dir O u_peri_subsys/uart0_sout
connect_bd_net [get_bd_pins u_peri_subsys/uart0_sout] [get_bd_pins u_peri_subsys/axi_uart16550_0/sout]
create_bd_pin -dir I u_peri_subsys/uart0_sin
connect_bd_net [get_bd_pins u_peri_subsys/uart0_sin] [get_bd_pins u_peri_subsys/axi_uart16550_0/sin]
create_bd_pin -dir I u_peri_subsys/ACLK_NOC
connect_bd_net [get_bd_pins u_peri_subsys/ACLK_NOC] [get_bd_pins u_peri_subsys/axi_interconnect_0/aclk]
create_bd_pin -dir I u_peri_subsys/ACLK_PERI
connect_bd_net [get_bd_pins u_peri_subsys/ACLK_PERI] [get_bd_pins u_peri_subsys/axi_interconnect_0/aclk1]
create_bd_pin -dir I u_peri_subsys/ARESETN
connect_bd_net [get_bd_pins u_peri_subsys/ARESETN] [get_bd_pins u_peri_subsys/reset_sync/ext_reset_in]
create_bd_pin -dir O u_peri_subsys/uart0_intc
connect_bd_net [get_bd_pins u_peri_subsys/uart0_intc] [get_bd_pins u_peri_subsys/axi_uart16550_0/ip2intc_irpt]
create_bd_intf_pin -mode Slave -vlnv xilinx.com:interface:aximm_rtl:1.0 u_peri_subsys/S_AXI_CFG
connect_bd_intf_net [get_bd_intf_pins u_peri_subsys/S_AXI_CFG] -boundary_type upper [get_bd_intf_pins u_peri_subsys/axi_interconnect_0/S00_AXI]

# u_peri_subsys connections
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/freeze]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/dsrn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_0/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/dcdn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_1/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/ctsn]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_1/dout] [get_bd_pins u_peri_subsys/axi_uart16550_0/rin]

connect_bd_intf_net [get_bd_intf_pins u_peri_subsys/axi_interconnect_0/M00_AXI] [get_bd_intf_pins u_peri_subsys/axi_uart16550_0/S_AXI]
connect_bd_intf_net [get_bd_intf_pins u_peri_subsys/axi_interconnect_0/M01_AXI] [get_bd_intf_pins u_peri_subsys/axi_bram_ctrl_0/S_AXI]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK_PERI] [get_bd_pins u_peri_subsys/reset_sync/slowest_sync_clk]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK_PERI] [get_bd_pins u_peri_subsys/axi_uart16550_0/s_axi_aclk]
connect_bd_net [get_bd_pins u_peri_subsys/ACLK_PERI] [get_bd_pins u_peri_subsys/axi_bram_ctrl_0/s_axi_aclk]

connect_bd_net [get_bd_pins u_peri_subsys/reset_sync/peripheral_aresetn] [get_bd_pins u_peri_subsys/axi_bram_ctrl_0/s_axi_aresetn]
connect_bd_net [get_bd_pins u_peri_subsys/reset_sync/peripheral_aresetn] [get_bd_pins u_peri_subsys/axi_uart16550_0/s_axi_aresetn]
connect_bd_net [get_bd_pins u_peri_subsys/reset_sync/interconnect_aresetn] [get_bd_pins u_peri_subsys/axi_interconnect_0/aresetn]

connect_bd_intf_net [get_bd_intf_pins u_peri_subsys/axi_bram_ctrl_0/BRAM_PORTA] [get_bd_intf_pins u_peri_subsys/boot_mem/BRAM_PORTA]
connect_bd_net [get_bd_pins u_peri_subsys/xlconstant_1/dout] [get_bd_pins u_peri_subsys/boot_mem/regcea]

regenerate_bd_layout -hierarchy [get_bd_cells u_peri_subsys]