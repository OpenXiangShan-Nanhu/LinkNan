set noc_clk_period 10.000
set cpu_clk_period 10.000
create_clock -name noc_clk   -period $noc_clk_period [get_ports io_noc_clock]
create_clock -name cpu_0_clk -period $cpu_clk_period [get_ports io_cluster_clocks_0]
create_clock -name cpu_1_clk -period $cpu_clk_period [get_ports io_cluster_clocks_1]
create_clock -name cpu_2_clk -period $cpu_clk_period [get_ports io_cluster_clocks_2]
create_clock -name cpu_3_clk -period $cpu_clk_period [get_ports io_cluster_clocks_3]
create_generated_clock -name dev_clk -divide_by 2 \
-source [get_pins */crg/clk_div_2/out*/C] \
[get_pins */crg/clk_div_2/out*/Q]

set hnx_dat      */noc/hnf_*/hnx/dataBlock/dataStorage_*/array
set_property DONT_TOUCH TRUE [get_cells */crg/clk_div_2/*_reg*]

set_property DONT_TOUCH TRUE [get_cells $hnx_dat/addr*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/data_*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/wreqReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/rreqReg*_reg*]