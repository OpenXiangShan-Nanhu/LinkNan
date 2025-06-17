create_pblock pblock_ddr
add_cells_to_pblock [get_pblocks pblock_ddr] [get_cells -quiet [list ln_simple_i/u_jtag_ddr_subsys]]
resize_pblock [get_pblocks pblock_ddr] -add {CLOCKREGION_X0Y11:CLOCKREGION_X1Y14}
create_pblock pblock_peri
add_cells_to_pblock [get_pblocks pblock_peri] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/iowrp_east ln_simple_i/u_peri_subsys]]
resize_pblock [get_pblocks pblock_peri] -add {CLOCKREGION_X8Y12:CLOCKREGION_X8Y14}

create_pblock pblock_hnf_0
add_cells_to_pblock [get_pblocks pblock_hnf_0] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_0]]
resize_pblock [get_pblocks pblock_hnf_0] -add {CLOCKREGION_X4Y17:CLOCKREGION_X6Y18}
create_pblock pblock_hnf_1
add_cells_to_pblock [get_pblocks pblock_hnf_1] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_1]]
resize_pblock [get_pblocks pblock_hnf_1] -add {CLOCKREGION_X4Y15:CLOCKREGION_X6Y16}
create_pblock pblock_hnf_2
add_cells_to_pblock [get_pblocks pblock_hnf_2] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_2]]
resize_pblock [get_pblocks pblock_hnf_2] -add {CLOCKREGION_X4Y13:CLOCKREGION_X6Y14}
create_pblock pblock_hnf_3
add_cells_to_pblock [get_pblocks pblock_hnf_3] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_3]]
resize_pblock [get_pblocks pblock_hnf_3] -add {CLOCKREGION_X4Y11:CLOCKREGION_X6Y12}
create_pblock pblock_hub
add_cells_to_pblock [get_pblocks pblock_hub] [get_cells -quiet [list ln_simple_i/ln/inst/soc/cc_0/hub]]
resize_pblock [get_pblocks pblock_hub] -add {CLOCKREGION_X2Y10:CLOCKREGION_X2Y10}
create_pblock pblock_tile
add_cells_to_pblock [get_pblocks pblock_tile] [get_cells -quiet [list ln_simple_i/core_bufg ln_simple_i/ln/inst/soc/cc_0/tile]]
resize_pblock [get_pblocks pblock_tile] -add {CLOCKREGION_X0Y3:CLOCKREGION_X8Y9}
create_pblock pblock_rs_0x0
add_cells_to_pblock [get_pblocks pblock_rs_0x0] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x0 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x0_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x0] -add {SLICE_X167Y1140:SLICE_X179Y1158}
create_pblock pblock_rs_0x8
add_cells_to_pblock [get_pblocks pblock_rs_0x8] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x8 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x8_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x8] -add {SLICE_X265Y1140:SLICE_X280Y1158}
create_pblock pblock_rs_0x10
add_cells_to_pblock [get_pblocks pblock_rs_0x10] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x10 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x10_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x10] -add {SLICE_X366Y1140:SLICE_X380Y1159}
create_pblock pblock_rs_0x18
add_cells_to_pblock [get_pblocks pblock_rs_0x18] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x18 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x18_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x18] -add {SLICE_X349Y1050:SLICE_X397Y1078}
resize_pblock [get_pblocks pblock_rs_0x18] -add {DSP48E2_X6Y420:DSP48E2_X6Y429}
resize_pblock [get_pblocks pblock_rs_0x18] -add {RAMB18_X6Y420:RAMB18_X6Y429}
resize_pblock [get_pblocks pblock_rs_0x18] -add {RAMB36_X6Y210:RAMB36_X6Y214}
create_pblock pblock_rs_0x20
add_cells_to_pblock [get_pblocks pblock_rs_0x20] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x20 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x20_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x20] -add {SLICE_X349Y960:SLICE_X397Y989}
resize_pblock [get_pblocks pblock_rs_0x20] -add {RAMB18_X6Y384:RAMB18_X6Y395}
resize_pblock [get_pblocks pblock_rs_0x20] -add {RAMB36_X6Y192:RAMB36_X6Y197}
create_pblock pblock_rs_0x28
add_cells_to_pblock [get_pblocks pblock_rs_0x28] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_2_id_0x28 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_2_id_0x28_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x28] -add {SLICE_X349Y870:SLICE_X397Y898}
resize_pblock [get_pblocks pblock_rs_0x28] -add {RAMB18_X6Y348:RAMB18_X6Y357}
resize_pblock [get_pblocks pblock_rs_0x28] -add {RAMB36_X6Y174:RAMB36_X6Y178}
create_pblock pblock_rs_0x30
add_cells_to_pblock [get_pblocks pblock_rs_0x30] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_rnh_id_0x30 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_rnh_id_0x30_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x30] -add {SLICE_X349Y810:SLICE_X397Y839}
resize_pblock [get_pblocks pblock_rs_0x30] -add {RAMB18_X6Y324:RAMB18_X6Y335}
resize_pblock [get_pblocks pblock_rs_0x30] -add {RAMB36_X6Y162:RAMB36_X6Y167}
create_pblock pblock_rs_0x38
add_cells_to_pblock [get_pblocks pblock_rs_0x38] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hni_id_0x38 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hni_id_0x38_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x38] -add {SLICE_X349Y750:SLICE_X397Y778}
resize_pblock [get_pblocks pblock_rs_0x38] -add {RAMB18_X6Y300:RAMB18_X6Y309}
resize_pblock [get_pblocks pblock_rs_0x38] -add {RAMB36_X6Y150:RAMB36_X6Y154}
create_pblock pblock_rs_0x40
add_cells_to_pblock [get_pblocks pblock_rs_0x40] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_3_id_0x40 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_3_id_0x40_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x40] -add {SLICE_X349Y690:SLICE_X397Y719}
resize_pblock [get_pblocks pblock_rs_0x40] -add {RAMB18_X6Y276:RAMB18_X6Y287}
resize_pblock [get_pblocks pblock_rs_0x40] -add {RAMB36_X6Y138:RAMB36_X6Y143}
create_pblock pblock_rs_0x48
add_cells_to_pblock [get_pblocks pblock_rs_0x48] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x48 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x48_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x48] -add {SLICE_X365Y630:SLICE_X380Y659}
create_pblock pblock_rs_0x50
add_cells_to_pblock [get_pblocks pblock_rs_0x50] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x50 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x50_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x50] -add {SLICE_X266Y630:SLICE_X281Y659}

create_pblock pblock_iowrp_west
add_cells_to_pblock [get_pblocks pblock_iowrp_west] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/iowrp_west]]
resize_pblock [get_pblocks pblock_iowrp_west] -add {CLOCKREGION_X1Y13:CLOCKREGION_X2Y14}
set_property C_CLK_INPUT_FREQ_HZ 300000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER false [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets clk]
