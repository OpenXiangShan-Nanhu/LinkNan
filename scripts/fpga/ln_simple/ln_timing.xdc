

set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/uncore/noc/hnf_*]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/uncore/noc/ring/*]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/uncore/noc/iowrp_*]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/u_jtag_ddr_subsys]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/u_peri_subsys]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/cc_*]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/cc_*/tile]
set_property KEEP_HIERARCHY TRUE [get_cells ln_simple_i/ln/inst/soc/cc_*/hub]

# Misc timing exception
set_false_path -from [get_pins ln_simple_i/ln/inst/_rtc_reg*/C]
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/cg/active*/C] 2

# LLC data ram input MCP
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/addr*/C] 2
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/data_*/C] 2
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/wreqReg*[0]/C] 2
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/rreqReg*[0]/C] 2
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/activeReg*[0]/C] 2

# Core Async
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_src_*/mem*/C] 25.00
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_src_*/widx_gray*/C] 12.50
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_sink_*/ridx_gray*/C] 12.50

set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_src_*/mem*/C] 25.00
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_src_*/widx_gray*/C] 12.50
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_sink_*/ridx_gray*/C] 12.50

set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/timerSource/mem*/C] 25.00

set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/io_cpu_meip*/C]
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/io_cpu_seip*/C]
# set_false_path -from [get_pins $cc_path/hub/io_cpu_dbip*/C]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/io_cpu_meip*/D]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/io_cpu_seip*/D]
# set_false_path -to [get_pins $cc_path/hub/io_cpu_dbip*/D]

set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/reqToOn*/D]

# PPU Timing exception
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/cpc/pSlv/pdenied*/C]
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/cpc/pSlv/paccept*/C]
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/cpc/pSlv/pactive*/C]
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_0/hub/clusterPeriCx/cpu_pwr_ctl_0/pcu/devMst/preq*/C] 12.50
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_0/hub/clusterPeriCx/cpu_pwr_ctl_0/pcu/devMst/pstate*/C] 12.50

