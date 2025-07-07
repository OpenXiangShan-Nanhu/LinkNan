create_clock -name noc_clk -period 10.000 [get_ports icn_noc_clock]
create_clock -name cpu_clk -period 10.000 [get_ports icn_cpu_clock]

# Interrupt and RTC exception
set_property DONT_TOUCH TRUE [get_cells */hub/io_cpu_meip*_reg]
set_property DONT_TOUCH TRUE [get_cells */hub/io_cpu_seip*_reg]
set_property DONT_TOUCH TRUE [get_cells */hub/io_cpu_dbip*_reg]

set_property DONT_TOUCH TRUE [get_cells */hub/clusterPeriCx/cpu_daclint_*/rtcSampler*_reg*]
set_property DONT_TOUCH TRUE [get_cells */tile/intBuffer*/*/*/sync_*_reg]

# PPU Timing Exception
set_property DONT_TOUCH TRUE [get_cells */hub/reqToOn*_reg]
set_property DONT_TOUCH TRUE [get_cells */hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/paccept*_reg]
set_property DONT_TOUCH TRUE [get_cells */hub/clusterPeriCx/cpu_pwr_ctl_*/pcu/devMst/pdenied*_reg]
set_property DONT_TOUCH TRUE [get_cells */hub/clusterPeriCx/cpu_pwr_ctl_*/pcsm/ctrl/ctrlState_fnEn*_reg]