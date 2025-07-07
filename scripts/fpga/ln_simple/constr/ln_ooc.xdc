# Create clocks
set ln_path */soc
create_clock -name noc_clk -period 10.000 [get_ports io_aclk]
create_clock -name cpu_clk -period 10.000 [get_ports io_core_clk_0]
create_clock -name rtc_clk -period 100.00 [get_ports io_rtc_clk]
create_generated_clock -name dev_clk -divide_by 2 \
-source [get_pins $ln_path/uncore/crg/clk_div_2/out*/C] \
[get_pins $ln_path/uncore/crg/clk_div_2/out*/Q]

set_clock_groups -name async_core_noc -asynchronous \
-group [get_clocks noc_clk] \
-group [get_clocks cpu_clk] \
-group [get_clocks dev_clk]

set tl_dev_wrp $ln_path/uncore/devWrp/tlDevBlock

set cfg_src  $tl_dev_wrp/cfgAsyncSrc
set cfg_sink $tl_dev_wrp/inner/tlAsyncSink
set sba_src  $tl_dev_wrp/inner/tlAsyncSrc
set sba_sink $tl_dev_wrp/sbaAsyncSink

set cfg_src_a  $cfg_src/nodeOut_a_source
set cfg_src_d  $cfg_src/nodeIn_d_sink
set cfg_sink_a $cfg_sink/nodeOut_a_sink
set cfg_sink_d $cfg_sink/nodeIn_d_source
set sba_src_a  $sba_src/nodeOut_a_source
set sba_src_d  $sba_src/nodeIn_d_sink
set sba_sink_a $sba_sink/nodeOut_a_sink
set sba_sink_d $sba_sink/nodeIn_d_source

set hub_pdc  $ln_path/cc_*/hub/pdc
set tile_pdc $ln_path/cc_*/tile/pdc

set tx_src   $tile_pdc/async_src_*
set tx_sink  $hub_pdc/async_sink_*
set rx_src   $hub_pdc/async_src_*
set rx_sink  $tile_pdc/async_sink_*

set timer_src  $ln_path/cc_*/hub/timerSource
set timer_sink $ln_path/cc_*/tile/timerSink

set hnx_dat      $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array

set_property ASYNC_REG TRUE [get_cells $cfg_src_a/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_src_a/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_sink_a/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_sink_a/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $cfg_sink_d/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_sink_d/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_src_d/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_src_d/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $sba_src_a/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_src_a/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_sink_a/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_sink_a/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $tx_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $tx_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $tx_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $tx_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $rx_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $rx_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $rx_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $rx_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $timer_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $timer_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $timer_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $timer_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

# Interrupt and RTC exception
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_meip*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_seip*_reg]
# set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/io_cpu_dbip*_reg]

set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_daclint_*/rtcSampler*_reg*]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/tile/intBuffer*/*/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/uncore/rtcSync_sync/sync_*_reg]

# PPU Timing Exception
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/tile/cpc/pSlv/asyncSink/vld_sync/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/reqToOn*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg]
set_property ASYNC_REG TRUE [get_cells $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg]

# Keep Hierarchy
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/noc/hnf_*]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/noc/iowrp_west]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/noc/iowrp_east]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/devWrp]
set_property KEEP_HIERARCHY TRUE [get_cells $hnx_dat]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/noc/ring/ring_stop_*]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/cc_*]

# Dont Touch
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/addr*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/data_*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/wreqReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/rreqReg*_reg*]