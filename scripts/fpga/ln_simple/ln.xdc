
set_property PACKAGE_PIN AD13 [get_ports uart0_sout]
set_property PACKAGE_PIN AE13 [get_ports uart0_sin]
set_property PACKAGE_PIN CC35 [get_ports led3]
set_property PACKAGE_PIN C35 [get_ports core_p]
set_property PACKAGE_PIN C36 [get_ports core_n]
set_property PACKAGE_PIN Y52 [get_ports ddr_p]
set_property PACKAGE_PIN Y53 [get_ports ddr_n]

set_property IOSTANDARD LVDS [get_ports ddr_p]
set_property IOSTANDARD LVDS [get_ports ddr_n]
set_property IOSTANDARD LVDS [get_ports core_p]
set_property IOSTANDARD LVDS [get_ports core_n]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_sout]
set_property IOSTANDARD LVCMOS33 [get_ports uart0_sin]
set_property IOSTANDARD LVCMOS18 [get_ports led3]

create_clock -period 20.000 -name CORE_CLK_IN [get_ports core_p]