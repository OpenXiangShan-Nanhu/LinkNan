# Clocks
set ln_path */ln/inst/soc
set cc_path $ln_path/cc_*/inst
set uc_path $ln_path/uncore/inst

create_generated_clock -name dev_clk -divide_by 2 \
-source [get_pins $uc_path/crg/clk_div_2/out*/C] \
[get_pins $uc_path/crg/clk_div_2/out*/Q]

set_clock_groups -name async_ln -asynchronous \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/peri_clk]] \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/rtc_clk]] \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/sys_clk]]

# LLC timing expcetions
set hnx_dat      $uc_path/noc/hnf_*/hnx/dataBlock/dataStorage_*/array

# LLC data ram is implemeted with URAM, s2h1l2
set_multicycle_path -setup -from [get_pins $hnx_dat/addr*/C]  -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/ADDR*] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/addr*/C]  -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/ADDR*] 1
set_multicycle_path -setup -from [get_pins $hnx_dat/data_*/C] -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/DIN*]  2
set_multicycle_path -hold  -from [get_pins $hnx_dat/data_*/C] -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/DIN*]  1
set_multicycle_path -setup -from [get_pins $hnx_dat/ram/wreqReg*[0]/C] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/ram/wreqReg*[0]/C] 1
set_multicycle_path -setup -from [get_pins $hnx_dat/ram/rreqReg*[0]/C] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/ram/rreqReg*[0]/C] 1

# Core Async Constraints
set hub_pdc $cc_path/hub/pdc
set tile_pdc $cc_path/tile/pdc

set tx_src   $tile_pdc/async_src_*
set tx_sink  $hub_pdc/async_sink_*
set rx_src   $hub_pdc/async_src_*
set rx_sink  $tile_pdc/async_sink_*

# Interrupt and RTC exception
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/io_cpu_meip*_reg]
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/io_cpu_seip*_reg]
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/io_cpu_dbip*_reg]
set_false_path -to [get_pins $cc_path/hub/io_cpu_meip*_reg/D]
set_false_path -to [get_pins $cc_path/hub/io_cpu_seip*_reg/D]
set_false_path -to [get_pins $cc_path/hub/io_cpu_dbip*_reg/D]

set_property ASYNC_REG TRUE [get_cells $cc_path/hub/clusterPeriCx/cpu_daclint_*/rtcSampler*_reg*]
set_property ASYNC_REG TRUE [get_cells $cc_path/tile/intBuffer*/*/*/sync_*_reg]
set_false_path -to [get_pins $cc_path/tile/intBuffer*/*/*/sync_*_reg/D]

# PPU Timing exception
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/reqToOn*_reg]
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg]
set_property ASYNC_REG TRUE [get_cells $cc_path/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg]
set_false_path -from [get_pins $cc_path/hub/clusterPeriCx/cpu_pwr_ctl_*/pcsm/ctrl/ctrlState_fnEn*/C]
set_false_path -to [get_pins $cc_path/hub/reqToOn*_reg/D]
set_false_path -to [get_pins $cc_path/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg/D]
set_false_path -to [get_pins $cc_path/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg/D]