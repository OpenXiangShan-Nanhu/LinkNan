create_clock -name noc_clk -period 10.000 [get_ports icn_noc_clock]
create_clock -name cpu_clk -period 10.000 [get_ports icn_cpu_clock]

set_clock_groups -name async_core_noc -asynchronous \
-group [get_clocks noc_clk] \
-group [get_clocks cpu_clk]

set hub_pdc */hub/pdc
set tile_pdc */tile/pdc

set tx_src   $tile_pdc/async_src_*
set tx_sink  $hub_pdc/async_sink_*
set rx_src   $hub_pdc/async_src_*
set rx_sink  $tile_pdc/async_sink_*

set timer_src  */hub/timerSource
set timer_sink */tile/timerSink

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
set_property ASYNC_REG TRUE [get_cells */hub/io_cpu_meip*_reg]
set_property ASYNC_REG TRUE [get_cells */hub/io_cpu_seip*_reg]
# set_property ASYNC_REG TRUE [get_cells */hub/io_cpu_dbip*_reg]

set_property ASYNC_REG TRUE [get_cells */hub/clusterPeriCx/cpu_daclint_*/rtcSampler*_reg*]
set_property ASYNC_REG TRUE [get_cells */tile/intBuffer*/*/*/sync_*_reg]

# PPU Timing Exception
set_property ASYNC_REG TRUE [get_cells */tile/cpc/pSlv/asyncSink/vld_sync/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells */tile/cpc/pSlv/asyncSink/sink_d*_reg*]
set_property ASYNC_REG TRUE [get_cells */hub/reqToOn*_reg]
set_property ASYNC_REG TRUE [get_cells */hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg]
set_property ASYNC_REG TRUE [get_cells */hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg]