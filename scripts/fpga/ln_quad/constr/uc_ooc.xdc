create_clock -name noc_clk -period 10.000 [get_ports io_noc_clock]
create_generated_clock -name dev_clk -divide_by 2 \
-source [get_pins */crg/clk_div_2/out*/C] \
[get_pins */crg/clk_div_2/out*/Q]

set hnx_dat      */noc/hnf_*/hnx/dataBlock/dataStorage_*/array
set_property DONT_TOUCH TRUE [get_cells */crg/clk_div_2/*_reg*]

set_property DONT_TOUCH TRUE [get_cells $hnx_dat/addr*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/data_*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/wreqReg*_reg*]
set_property DONT_TOUCH TRUE [get_cells $hnx_dat/ram/rreqReg*_reg*]