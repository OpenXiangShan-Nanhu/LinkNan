# Clocks

set ln_path ln_simple_i/ln/inst/soc

set_clock_groups -name async_ln -asynchronous \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/peri_clk]] \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/rtc_clk]] \
-group [get_clocks -include_generated_clocks -of_objects [get_pins */in_mmcm/sys_clk]]

# LLC timing expcetions
set hnx          $ln_path/uncore/noc/hnf_*/hnx
set hnx_dat      $hnx/dataBlock/dataStorage_*/array

# LLC data ram is implemeted with URAM, s2h1l2
set_multicycle_path -setup -from [get_pins $hnx_dat/addr*/C]  -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/ADDR*] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/addr*/C]  -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/ADDR*] 1
set_multicycle_path -setup -from [get_pins $hnx_dat/data_*/C] -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/DIN*]  2
set_multicycle_path -hold  -from [get_pins $hnx_dat/data_*/C] -to [get_pins $hnx_dat/ram/array/mem/*mem_reg*/DIN*]  1
set_multicycle_path -setup -from [get_pins $hnx_dat/ram/wreqReg*[0]/C] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/ram/wreqReg*[0]/C] 1
set_multicycle_path -setup -from [get_pins $hnx_dat/ram/rreqReg*[0]/C] 2
set_multicycle_path -hold  -from [get_pins $hnx_dat/ram/rreqReg*[0]/C] 1

# Interrupt and RTC exception
set_false_path -to [get_pins $ln_path/cc_*/hub/io_cpu_meip*_reg/D]
set_false_path -to [get_pins $ln_path/cc_*/hub/io_cpu_seip*_reg/D]
# set_false_path -to [get_pins $ln_path/cc_*/hub/io_cpu_dbip*_reg/D]

set_false_path -to [get_pins $ln_path/cc_*/tile/intBuffer*/*/*/sync_*_reg/D]
set_false_path -to [get_pins $ln_path/uncore/rtcSync_sync/sync_*_reg/D]

# PPU Timing exception
set_false_path -to [get_pins $ln_path/cc_*/hub/reqToOn*_reg/D]
set_false_path -to [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg/D]
set_false_path -to [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg/D]
set_false_path -from [get_pins $ln_path/cc_*/hub/clusterPeriCx/cpu_pwr_ctl_*/pcsm/ctrl/ctrlState_fnEn*/C]