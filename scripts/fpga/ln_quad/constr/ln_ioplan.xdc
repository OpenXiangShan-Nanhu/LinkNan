# Global Clock 5
set_property PACKAGE_PIN CP33  [get_ports sys_clk_p]
set_property PACKAGE_PIN CR33  [get_ports sys_clk_n]
set_property IOSTANDARD LVDS15 [get_ports sys_clk_p]
set_property IOSTANDARD LVDS15 [get_ports sys_clk_n]

# UART
set_property PACKAGE_PIN CJ4 [get_ports uart0_sin]
set_property PACKAGE_PIN CJ3 [get_ports uart0_sout]
set_property IOSTANDARD LVCMOS15 [get_ports uart0_*]