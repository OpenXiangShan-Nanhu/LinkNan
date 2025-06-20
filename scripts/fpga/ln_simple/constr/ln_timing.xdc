# Misc timing exception
create_generated_clock -name dev_clk -divide_by 2 \
-source [get_pins ln_simple_i/ln/inst/soc/uncore/crg/clk_div_2/out*/C] \
[get_pins ln_simple_i/ln/inst/soc/uncore/crg/clk_div_2/out*/Q]

set_false_path -from [get_pins ln_simple_i/ln/inst/_rtc_reg*/C]
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/cg/active*/C] 2

# LLC data ram input MCP
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/addr*/C] 2
set_multicycle_path -from [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/data_*/C] 2
set_multicycle_path -from [get_pins {ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/wreqReg*[0]/C}] 2
set_multicycle_path -from [get_pins {ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/rreqReg*[0]/C}] 2
set_multicycle_path -from [get_pins {ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/activeReg*[0]/C}] 2

set_multicycle_path -to [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/directory/llcs_*/metaArray/dataReg*/D] 2
set_multicycle_path -to [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/directory/llcs_*/tagArray/dataReg*/D] 2
set_multicycle_path -to [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/directory/sfs_*/metaArray/dataReg*/D] 2
set_multicycle_path -to [get_pins ln_simple_i/ln/inst/soc/uncore/noc/hnf_*/hnx/directory/sfs_*/tagArray/dataReg*/D] 2

# Core Async
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_src_*/mem*/C] 25.000
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_src_*/widx_gray*/C] 12.500
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_sink_*/ridx_gray*/C] 12.500

set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_src_*/mem*/C] 25.000
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_src_*/widx_gray*/C] 12.500
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_sink_*/ridx_gray*/C] 12.500

set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/timerSource/mem*/C] 25.000

set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_src_*/sink_extend/io_out_sink_valid_0/output_chain/sync_2*/D]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/pdc/async_sink_*/source_extend/io_out_sink_valid_0/output_chain/sync_2*/D]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_src_*/sink_extend/io_out_sink_valid_0/output_chain/sync_2*/D]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/pdc/async_sink_*/source_extend/io_out_sink_valid_0/output_chain/sync_2*/D]

set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/timerSource/sink_extend/io_out_sink_valid_0/output_chain/sync_2*/D]
set_false_path -to [get_pins ln_simple_i/ln/inst/soc/cc_*/tile/timerSink/source_extend/io_out_sink_valid_0/output_chain/sync_2*/D]

# Core Misc
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
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/preq*/C] 12.500
set_max_delay -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pstate*/C] 12.500
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/clusterPeriCx/cpu_boot_ctl_*/addrReg*/C]
set_false_path -from [get_pins ln_simple_i/ln/inst/soc/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcsm/ctrl/ctrlState_fnEn*/C]

