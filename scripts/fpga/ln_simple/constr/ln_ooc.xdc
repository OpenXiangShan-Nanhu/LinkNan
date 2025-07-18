# Create clocks
set ln_path */soc
create_clock -name noc_clk -period 8.000 [get_ports io_aclk]
create_clock -name cpu_clk -period 8.000 [get_ports io_core_clk_0]
create_clock -name rtc_clk -period 100.00 [get_ports io_rtc_clk]

set hnx_dat $ln_path/uncore/noc/hnf_*/hnx/dataBlock/dataStorage_*/array

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
set_property KEEP_HIERARCHY TRUE [get_cells $hnx_dat]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/uncore/noc/ring/ring_stop_*]
set_property KEEP_HIERARCHY TRUE [get_cells $ln_path/cc_*]

# Dont Touch
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/addr*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/data_*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/wreqReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/rreqReg*_reg*]