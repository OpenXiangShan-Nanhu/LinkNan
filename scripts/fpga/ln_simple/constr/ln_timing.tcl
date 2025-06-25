# Clocks
set ln_path ln_simple_i/ln/inst/soc

set noc_clk_period [get_property PERIOD [get_clocks noc_clk_ln_simple_noc_pll_0]]
set cpu_clk_period [get_property PERIOD [get_clocks core_clk_ln_simple_core_pll_0]]
set min_clk_period [expr {min($noc_clk_period, $cpu_clk_period)}]

create_clock -name dev_clk -period [expr $noc_clk_period *2] [get_pins $ln_path/uncore/crg/clk_div_2/out*/Q]
set dev_clk_period [get_property PERIOD [get_clocks dev_clk]]

set_clock_groups -name async_ln -asynchronous \
-group [get_clocks noc_clk_ln_simple_noc_pll_0] \
-group [get_clocks core_clk_ln_simple_core_pll_0] \
-group [get_clocks dev_clk]

# LLC timing expcetions
# set_multicycle_path -setup -from [get_pins $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/addr*/C] 2
# set_multicycle_path -hold  -from [get_pins $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/addr*/C] 1
# set_multicycle_path -setup -from [get_pins $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/data_*/C] 2
# set_multicycle_path -hold  -from [get_pins $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/data_*/C] 1
# set_multicycle_path -setup -from [get_pins {$ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/wreqReg*[0]/C}] 2
# set_multicycle_path -hold  -from [get_pins {$ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/wreqReg*[0]/C}] 1
# set_multicycle_path -setup -from [get_pins {$ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/rreqReg*[0]/C}] 2
# set_multicycle_path -hold  -from [get_pins {$ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array/ram/rreqReg*[0]/C}] 1

# Device Wrapper Async Constraints
set tl_dev_wrp $ln_path/uncore/devWrp/tlDevBlock

set cfg_mst   $tl_dev_wrp/cfgAsyncSrc
set cfg_slv   $tl_dev_wrp/inner/tlAsyncSink
set sba_mst   $tl_dev_wrp/inner/tlAsyncSrc
set sba_slv   $tl_dev_wrp/sbaAsyncSink

set cfg_a_src  $cfg_mst/nodeOut_a_source
set cfg_a_sink $cfg_slv/nodeOut_a_sink
set cfg_d_src  $cfg_slv/nodeIn_d_source
set cfg_d_sink $cfg_mst/nodeIn_d_sink
set sba_a_src  $sba_mst/nodeOut_a_source
set sba_a_sink $sba_slv/nodeOut_a_sink
set sba_d_src  $sba_slv/nodeIn_d_source
set sba_d_sink $sba_mst/nodeIn_d_sink

proc dev_wrp_async {src sink} {
  set src_clk_period  [get_property PERIOD [get_clocks -of_object [lindex [get_pins $src/sink_extend/*/*/sync_*_reg/C] 0]]]
  set sink_clk_period [get_property PERIOD [get_clocks -of_object [lindex [get_pins $sink/source_extend/*/*/sync_*_reg/C] 0]]]
  set dat_clk_period  [expr $sink_clk_period *2]
  set skew_period     [expr {min($src_clk_period, $sink_clk_period)}]
  set_max_delay $dat_clk_period  -from [get_pins $src/mem*/C]        -to [get_pins $sink/io_deq_bits*/cdc_reg*/D]        -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $src/mem*/C]        -to [get_pins $sink/io_deq_bits*/cdc_reg*/D]
  set_max_delay $sink_clk_period -from [get_pins $src/widx_gray*/C]  -to [get_pins $sink/widx_widx_gray/*/sync_*_reg*/D] -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $src/widx_gray*/C]  -to [get_pins $sink/widx_widx_gray/*/sync_*_reg*/D]
  set_max_delay $src_clk_period  -from [get_pins $sink/ridx_gray*/C] -to [get_pins $src/ridx_ridx_gray/*/sync_*_reg/D]   -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $sink/ridx_gray*/C] -to [get_pins $src/ridx_ridx_gray/*/sync_*_reg/D]
  set_property ASYNC_REG TRUE [get_cells $src/ridx_ridx_gray/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $sink/widx_widx_gray/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]
}
dev_wrp_async $cfg_a_src $cfg_a_sink
dev_wrp_async $cfg_d_src $cfg_d_sink
dev_wrp_async $sba_a_src $sba_a_sink
# dev_wrp_async $sba_d_src $sba_d_sink

# Core Async Constraints
set hub_pdc $ln_path/cc_*/hub/pdc
set tile_pdc $ln_path/cc_*/tile/pdc

set tx_src   $tile_pdc/async_src_*
set tx_sink  $hub_pdc/async_sink_*
set rx_src   $hub_pdc/async_src_*
set rx_sink  $tile_pdc/async_sink_*

proc cpu_chi_async {src sink} {
  set src_clk_period  [get_property PERIOD [get_clocks -of_object [lindex [get_pins $src/sink_extend/*/*/sync_*_reg/C] 0]]]
  set sink_clk_period [get_property PERIOD [get_clocks -of_object [lindex [get_pins $sink/source_extend/*/*/sync_*_reg/C] 0]]]
  set dat_clk_period  [expr $sink_clk_period *2]
  set skew_period     [expr {min($src_clk_period, $sink_clk_period)}]
  set_max_delay $dat_clk_period  -from [get_pins $src/mem_ext/Memory_*/RAM*/CLK] -to [get_pins $sink/io_deq_bits*/cdc_reg*/D] -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $src/mem_ext/Memory_*/RAM*/CLK] -to [get_pins $sink/io_deq_bits*/cdc_reg*/D]
  set_max_delay $sink_clk_period -from [get_pins $src/widx_gray*/C]  -to [get_pins $sink/widx_widx_gray/*/sync_*_reg*/D] -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $src/widx_gray*/C]  -to [get_pins $sink/widx_widx_gray/*/sync_*_reg*/D]
  set_max_delay $src_clk_period  -from [get_pins $sink/ridx_gray*/C] -to [get_pins $src/ridx_ridx_gray/*/sync_*_reg/D]   -datapath_only
  set_bus_skew  $skew_period     -from [get_pins $sink/ridx_gray*/C] -to [get_pins $src/ridx_ridx_gray/*/sync_*_reg/D]
  set_property ASYNC_REG TRUE [get_cells $src/ridx_ridx_gray/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $sink/widx_widx_gray/*/sync_*_reg]
  set_property ASYNC_REG TRUE [get_cells $sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]
}

cpu_chi_async $tx_src $tx_sink
cpu_chi_async $rx_src $rx_sink

# Core Timer Async Constraints
set timer_src $ln_path/cc_*/hub/timerSource
set timer_sink $ln_path/cc_*/tile/timerSink

dev_wrp_async $timer_src $timer_sink

# Interrupt and RTC exception
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_meip*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_seip*_reg]
# set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_dbip*_reg]

set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_daclint_*/rtcSampler*_reg*]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/tile/intBuffer*/*/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/uncore/rtcSync_sync/sync_*_reg]

# PPU Timing exception
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/tile/cpc/pSlv/asyncSink/vld_sync/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/reqToOn*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg]
set_false_path -from [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcsm/ctrl/ctrlState_fnEn*/C]
set_max_delay -datapath_only -from [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/asyncSrc/src_v*/C] $cpu_clk_period
set_max_delay -datapath_only -from [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/asyncSrc/src_d*/C] [expr $cpu_clk_period *2]
set_bus_skew $min_clk_period \
-from [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/asyncSrc/src_d*/C] \
-to [get_pins $ln_path/cc_*/tile/cpc/pSlv/asyncSink/sink_d*/D]