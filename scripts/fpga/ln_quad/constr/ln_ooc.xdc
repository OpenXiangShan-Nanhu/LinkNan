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

set_property ASYNC_REG TRUE [get_cells $cfg_a_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_a_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_a_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_a_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $cfg_d_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_d_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_d_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $cfg_d_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $sba_a_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_a_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_a_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_a_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set_property ASYNC_REG TRUE [get_cells $sba_d_src/ridx_ridx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_d_src/sink_extend/io_out_sink_valid_0/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_d_sink/widx_widx_gray/*/sync_*_reg]
set_property ASYNC_REG TRUE [get_cells $sba_d_sink/source_extend/io_out_sink_valid_0/*/sync_*_reg]

set hnx          $ln_path/uncore/noc/hnf_*/hnx
set hnx_dat      $hnx/dataBlock/dataStorage_*/array
set hnx_llc_tag  $hnx/directory/llcs*/tagArray
set hnx_llc_meta $hnx/directory/llcs*/metaArray
set hnx_sf_tag   $hnx/directory/sfs*/tagArray
set hnx_sf_meta  $hnx/directory/sfs*/metaArray

set_property ASYNC_REG TRUE [get_cells $ln_path/uncore/rtcSync_sync/sync_*_reg]
set_property DONT_TOUCH TRUE [get_cells $ln_path/uncore/crg/clk_div_2/*_reg*]

set_property DONT_TOUCH TRUE [get_cells $hnx_dat/addr*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/data_*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/dataReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/wreqReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/rreqReg*_reg*]

set_property DONT_TOUCH TRUE [get_cells $hnx_llc_tag/dataReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_llc_meta/dataReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_sf_tag/dataReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_sf_meta/dataReg*_reg*]