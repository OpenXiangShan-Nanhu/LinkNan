
set_property PACKAGE_PIN AD13 [get_ports uart0_sout]
set_property PACKAGE_PIN AE13 [get_ports uart0_sin]
set_property PACKAGE_PIN CC35 [get_ports led3]
set_property PACKAGE_PIN C35 [get_ports core_clk_p]
set_property PACKAGE_PIN C36 [get_ports core_clk_n]
set_property PACKAGE_PIN Y52 [get_ports ddr_clk_p]
set_property PACKAGE_PIN Y53 [get_ports ddr_clk_n]

set_property IOSTANDARD LVDS [get_ports ddr_clk_p]
set_property IOSTANDARD LVDS [get_ports ddr_clk_n]
set_property IOSTANDARD LVDS [get_ports core_clk_p]
set_property IOSTANDARD LVDS [get_ports core_clk_n]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_sout]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_sin]
set_property IOSTANDARD LVCMOS18 [get_ports led3]

set ln_top_path ln_simple_i/ln/inst
set hnf_path $ln_top_path/soc/uncore/noc/hnf_*
set cc_path $ln_top_path/soc/cc_*

set async_ptr_max_delay 1.250
set async_mem_max_delay [expr $async_ptr_max_delay *2]

# Misc timing exception
set_false_path -from [get_pins $ln_top_path/_rtc_reg*/C]
set_multicycle_path 2 -from [get_pins $hnf_path/cg/active*/C]

# LLC data ram MCP
set_multicycle_path 2 -from [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/addr*/C]
set_multicycle_path 2 -from [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/data_*/C]
set_multicycle_path 2 -from [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/ram/wreqReg*[0]/C]
set_multicycle_path 2 -from [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/ram/rreqReg*[0]/C]
set_multicycle_path 2 -from [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/ram/activeReg*[0]/C]
set_multicycle_path 2 -to [get_pins $hnf_path/hnx/dataBlock/dataStorage_*/array/dataReg*[0]/D]

# LLC dir ram MCP
set_multicycle_path 2 -to [get_pins $hnf_path/hnx/directory/llcs*/tagArray/dataReg*[0]/D]
set_multicycle_path 2 -to [get_pins $hnf_path/hnx/directory/llcs*/metaArray/dataReg*[0]/D]
set_multicycle_path 2 -to [get_pins $hnf_path/hnx/directory/sfs*/tagArray/dataReg*[0]/D]

# Core Async
set_max_delay $async_mem_max_delay -from [get_pins $cc_path/tile/pdc/async_src_*/mem*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $cc_path/tile/pdc/async_src_*/widx_gray*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $cc_path/tile/pdc/async_sink_*/ridx_gray*/C]

set_max_delay $async_mem_max_delay -from [get_pins $cc_path/hub/pdc/async_src_*/mem*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $cc_path/hub/pdc/async_src_*/widx_gray*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $cc_path/hub/pdc/async_sink_*/ridx_gray*/C]

set_max_delay $async_mem_max_delay -from [get_pins $cc_path/hub/timerSource/mem*/C]

set_false_path -from [get_pins $cc_path/hub/io_cpu_meip*/C]
set_false_path -from [get_pins $cc_path/hub/io_cpu_seip*/C]
# set_false_path -from [get_pins $cc_path/hub/io_cpu_dbip*/C]
set_multicycle_path 5 -to [get_pins $cc_path/hub/io_cpu_meip*/D]
set_multicycle_path 5 -to [get_pins $cc_path/hub/io_cpu_seip*/D]
# set_multicycle_path 5 -to [get_pins $cc_path/hub/io_cpu_dbip*/D]

set_false_path -to [get_pins $cc_path/hub/reqToOn*/D]

# PPU Timing exception
set_false_path -from [get_pins $cc_path/tile/cpc/pSlv/pdenied*/C]
set_false_path -from [get_pins $cc_path/tile/cpc/pSlv/paccept*/C]
set_false_path -from [get_pins $cc_path/tile/cpc/pSlv/pactive*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $ln_top_path/soc/cc_0/hub/clusterPeriCx/cpu_pwr_ctl_0/pcu/devMst/preq*/C]
set_max_delay $async_ptr_max_delay -from [get_pins $ln_top_path/soc/cc_0/hub/clusterPeriCx/cpu_pwr_ctl_0/pcu/devMst/pstate*/C]

# Place suggestion

set_property USER_CLUSTER uc_hnf_0 [get_cells $ln_top_path/soc/uncore/noc/hnf_0]
set_property USER_CLUSTER uc_hnf_1 [get_cells $ln_top_path/soc/uncore/noc/hnf_1]
set_property USER_CLUSTER uc_cc_0 [get_cells $ln_top_path/soc/cc_0]