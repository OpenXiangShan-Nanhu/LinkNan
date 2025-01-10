set project_name "cpu_die"
create_project $project_name $project_name -part xcvu19p-fsva3824-2-e
set_property simulator_language Verilog [current_project]

set core_fl [open "linknan/NanhuCoreWrapper.f" r]
set cluster_fl [open "linknan/CpuCluster.f" r]
set ln_path [pwd]
append ln_path "/linknan"

set all_files_raw [read $core_fl]
append all_files_raw [read $cluster_fl]
regsub -all {\$release_path} $all_files_raw $ln_path all_files
set cleaned_list {}
foreach item [split $all_files "\n"] {
  if {$item ne ""} {
    lappend cleaned_list $item
  }
}

add_files -norecurse -scan_for_includes $cleaned_list

set_property file_type Verilog [get_files CpuCluster.sv]

cd [join [list [pwd] $project_name] "/"]

create_bd_design $project_name
create_bd_cell -type ip -vlnv xilinx.com:ip:clk_wiz clk_wiz_0
create_bd_cell -type module -reference CpuCluster cc
create_bd_cell -type ip -vlnv xilinx.com:ip:aurora_64b66b c2c
create_bd_cell -type ip -vlnv xilinx.com:ip:vio rst_ctrl
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconstant const_0
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilvector_logic reverser
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilconcat rst_concat
create_bd_cell -type inline_hdl -vlnv xilinx.com:inline_hdl:ilreduced_logic rst_red_or

create_bd_port -dir I -type intr msip_0
create_bd_port -dir I -type intr msip_1
create_bd_port -dir I -type intr mtip_0
create_bd_port -dir I -type intr mtip_1
create_bd_port -dir I -type intr meip_0
create_bd_port -dir I -type intr meip_1
create_bd_port -dir I -type intr seip_0
create_bd_port -dir I -type intr seip_1
create_bd_port -dir I -type intr dbip_0
create_bd_port -dir I -type intr dbip_1
create_bd_port -dir O -type intr reset_state_0
create_bd_port -dir O -type intr reset_state_1
create_bd_port -dir I -from 7 -to 0 cluster_id
create_bd_port -dir I -from 47 -to 0 boot_addr
create_bd_port -dir I -type clk -freq_hz 100000000 osc_clock

set_property -dict [list \
  CONFIG.CLKOUT1_DRIVES {Buffer} \
  CONFIG.CLKOUT1_JITTER {144.719} \
  CONFIG.CLKOUT1_PHASE_ERROR {114.212} \
  CONFIG.CLKOUT2_DRIVES {Buffer} \
  CONFIG.CLKOUT3_DRIVES {Buffer} \
  CONFIG.CLKOUT4_DRIVES {Buffer} \
  CONFIG.CLKOUT5_DRIVES {Buffer} \
  CONFIG.CLKOUT6_DRIVES {Buffer} \
  CONFIG.CLKOUT7_DRIVES {Buffer} \
  CONFIG.MMCM_BANDWIDTH {OPTIMIZED} \
  CONFIG.MMCM_CLKFBOUT_MULT_F {8} \
  CONFIG.MMCM_CLKOUT0_DIVIDE_F {8} \
  CONFIG.MMCM_COMPENSATION {AUTO} \
  CONFIG.PRIMITIVE {PLL} \
  CONFIG.USE_FREQ_SYNTH {false} \
  CONFIG.USE_RESET {false} \
  CONFIG.USE_LOCKED {true} \
  CONFIG.OPTIMIZE_CLOCKING_STRUCTURE_EN {true} \
] [get_bd_cells clk_wiz_0]

set_property -dict [list \
  CONFIG.C_NUM_PROBE_IN {1} \
  CONFIG.C_NUM_PROBE_OUT {2} \
  CONFIG.C_PROBE_OUT0_INIT_VAL {0x1} \
  CONFIG.C_PROBE_OUT1_INIT_VAL {0x1} \
] [get_bd_cells rst_ctrl]

set_property -dict [list \
  CONFIG.C_AURORA_LANES {4} \
  CONFIG.SupportLevel {1} \
  CONFIG.interface_mode {Streaming} \
] [get_bd_cells c2c]

set_property -dict [list \
  CONFIG.CONST_VAL {0} \
  CONFIG.CONST_WIDTH {3} \
] [get_bd_cells const_0]

set_property -dict [list \
  CONFIG.C_OPERATION {not} \
  CONFIG.C_SIZE {1} \
] [get_bd_cells reverser]

set_property CONFIG.NUM_PORTS {3} [get_bd_cells rst_concat]

set_property -dict [list \
  CONFIG.C_OPERATION {or} \
  CONFIG.C_SIZE {3} \
] [get_bd_cells rst_red_or]

make_bd_intf_pins_external  [get_bd_intf_pins c2c/GT_DIFF_REFCLK1]
set_property name GT_REFCLK [get_bd_intf_ports GT_DIFF_REFCLK1_0]

make_bd_intf_pins_external  [get_bd_intf_pins c2c/GT_SERIAL_TX]
set_property name GT_TX [get_bd_intf_ports GT_SERIAL_TX_0]

make_bd_intf_pins_external  [get_bd_intf_pins c2c/GT_SERIAL_RX]
set_property name GT_RX [get_bd_intf_ports GT_SERIAL_RX_0]

connect_bd_net [get_bd_pins const_0/dout] [get_bd_pins cc/icn_dft_*]

connect_bd_net [get_bd_ports msip_0] [get_bd_pins cc/icn_misc_msip_0]
connect_bd_net [get_bd_ports msip_1] [get_bd_pins cc/icn_misc_msip_1]
connect_bd_net [get_bd_ports mtip_0] [get_bd_pins cc/icn_misc_mtip_0]
connect_bd_net [get_bd_ports mtip_1] [get_bd_pins cc/icn_misc_mtip_1]
connect_bd_net [get_bd_ports meip_0] [get_bd_pins cc/icn_misc_meip_0]
connect_bd_net [get_bd_ports meip_1] [get_bd_pins cc/icn_misc_meip_1]
connect_bd_net [get_bd_ports seip_0] [get_bd_pins cc/icn_misc_seip_0]
connect_bd_net [get_bd_ports seip_1] [get_bd_pins cc/icn_misc_seip_1]
connect_bd_net [get_bd_ports dbip_0] [get_bd_pins cc/icn_misc_dbip_0]
connect_bd_net [get_bd_ports dbip_1] [get_bd_pins cc/icn_misc_dbip_1]
connect_bd_net [get_bd_ports reset_state_0] [get_bd_pins cc/icn_misc_resetState_0]
connect_bd_net [get_bd_ports reset_state_1] [get_bd_pins cc/icn_misc_resetState_1]

connect_bd_net [get_bd_ports cluster_id] [get_bd_pins cc/icn_misc_clusterId]
connect_bd_net [get_bd_ports boot_addr] [get_bd_pins cc/icn_misc_defaultBootAddr]

connect_bd_net [get_bd_pins cc/icn_socket_c2c_tx_valid] [get_bd_pins c2c/s_axi_tx_tvalid]
connect_bd_net [get_bd_pins cc/icn_socket_c2c_tx_bits] [get_bd_pins c2c/s_axi_tx_tdata]
connect_bd_net [get_bd_pins c2c/s_axi_tx_tready] [get_bd_pins cc/icn_socket_c2c_tx_ready]
connect_bd_net [get_bd_pins c2c/m_axi_rx_tdata] [get_bd_pins cc/icn_socket_c2c_rx_bits]
connect_bd_net [get_bd_pins c2c/m_axi_rx_tvalid] [get_bd_pins cc/icn_socket_c2c_rx_valid]
connect_bd_net [get_bd_pins const_0/dout] [get_bd_pins cc/icn_socket_chipRx]

connect_bd_net [get_bd_ports osc_clock] [get_bd_pins clk_wiz_0/clk_in1]
connect_bd_net [get_bd_pins clk_wiz_0/clk_out1] [get_bd_pins c2c/init_clk]

connect_bd_net [get_bd_pins rst_ctrl/probe_out0] [get_bd_pins c2c/pma_init]
connect_bd_net [get_bd_pins rst_ctrl/probe_out1] [get_bd_pins c2c/reset_pb]
connect_bd_net [get_bd_pins clk_wiz_0/clk_out1] [get_bd_pins rst_ctrl/clk]
connect_bd_net [get_bd_pins clk_wiz_0/locked] [get_bd_pins rst_ctrl/probe_in0]

connect_bd_net [get_bd_pins const_0/dout] [get_bd_pins c2c/gt_rxcdrovrden_in]
connect_bd_net [get_bd_pins const_0/dout] [get_bd_pins c2c/loopback]
connect_bd_net [get_bd_pins const_0/dout] [get_bd_pins c2c/power_down]

connect_bd_net [get_bd_pins c2c/channel_up] [get_bd_pins reverser/Op1]
connect_bd_net [get_bd_pins reverser/Res] [get_bd_pins rst_concat/In0]
connect_bd_net [get_bd_pins c2c/mmcm_not_locked_out] [get_bd_pins rst_concat/In1]
connect_bd_net [get_bd_pins c2c/sys_reset_out] [get_bd_pins rst_concat/In2]
connect_bd_net [get_bd_pins rst_concat/dout] [get_bd_pins rst_red_or/Op1]
connect_bd_net [get_bd_pins rst_red_or/Res] [get_bd_pins cc/icn_socket_resetRx]
connect_bd_net [get_bd_pins c2c/user_clk_out] [get_bd_pins cc/icn_osc_clock]

regenerate_bd_layout -routing

set bd_dir [join [ list [format "%s.srcs" $project_name ] "sources_1" "bd" $project_name] "/"]
set bd_file [join [ list $bd_dir [format "%s.bd" $project_name] ] "/"]
set wrapper_dir [join [ list [format "%s.gen" $project_name ] "sources_1" "bd" $project_name] "/"]
set wrapper_file [join [ list $wrapper_dir "hdl" [format "%s_wrapper.v" $project_name] ] "/"]
make_wrapper -files [get_files $bd_file ] -top
add_files -norecurse $wrapper_file
set_property top [format "%s_wrapper" $project_name] [current_fileset]
update_compile_order -fileset sources_1

# synthesis
set_param synth.maxThreads 8
set_property STEPS.SYNTH_DESIGN.ARGS.GATED_CLOCK_CONVERSION on [get_runs synth_1]
set_property STEPS.SYNTH_DESIGN.ARGS.KEEP_EQUIVALENT_REGISTERS true [get_runs synth_1]
launch_runs synth_1 -jobs 8

cd ..