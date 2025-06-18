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
create_pblock pblock_tile
add_cells_to_pblock [get_pblocks pblock_tile] [get_cells -quiet [list ln_simple_i/ln/inst/soc/cc_0/tile]]
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
create_pblock pblock_rs_0x60
add_cells_to_pblock [get_pblocks pblock_rs_0x60] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_3_id_0x60 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_3_id_0x60_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x60] -add {SLICE_X149Y690:SLICE_X198Y719}
resize_pblock [get_pblocks pblock_rs_0x60] -add {RAMB18_X3Y276:RAMB18_X3Y287}
resize_pblock [get_pblocks pblock_rs_0x60] -add {RAMB36_X3Y138:RAMB36_X3Y143}
create_pblock pblock_rs_0x68
add_cells_to_pblock [get_pblocks pblock_rs_0x68] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_sn_id_0x68 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_sn_id_0x68_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x68] -add {SLICE_X149Y750:SLICE_X198Y779}
resize_pblock [get_pblocks pblock_rs_0x68] -add {RAMB18_X3Y300:RAMB18_X3Y311}
resize_pblock [get_pblocks pblock_rs_0x68] -add {RAMB36_X3Y150:RAMB36_X3Y155}
create_pblock pblock_rs_0x70
add_cells_to_pblock [get_pblocks pblock_rs_0x70] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_mn_id_0x70 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_mn_id_0x70_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x70] -add {SLICE_X149Y810:SLICE_X198Y839}
resize_pblock [get_pblocks pblock_rs_0x70] -add {RAMB18_X3Y324:RAMB18_X3Y335}
resize_pblock [get_pblocks pblock_rs_0x70] -add {RAMB36_X3Y162:RAMB36_X3Y167}
create_pblock pblock_rs_0x78
add_cells_to_pblock [get_pblocks pblock_rs_0x78] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_2_id_0x78 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_2_id_0x78_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x78] -add {SLICE_X149Y870:SLICE_X198Y899}
resize_pblock [get_pblocks pblock_rs_0x78] -add {RAMB18_X3Y348:RAMB18_X3Y359}
resize_pblock [get_pblocks pblock_rs_0x78] -add {RAMB36_X3Y174:RAMB36_X3Y179}
create_pblock pblock_rs_0x88
add_cells_to_pblock [get_pblocks pblock_rs_0x88] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x88 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x88_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x88] -add {SLICE_X149Y1050:SLICE_X198Y1079}
resize_pblock [get_pblocks pblock_rs_0x88] -add {RAMB18_X3Y420:RAMB18_X3Y431}
resize_pblock [get_pblocks pblock_rs_0x88] -add {RAMB36_X3Y210:RAMB36_X3Y215}
create_pblock pblock_rs_0x80
add_cells_to_pblock [get_pblocks pblock_rs_0x80] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x80 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x80_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x80] -add {SLICE_X149Y960:SLICE_X198Y989}
resize_pblock [get_pblocks pblock_rs_0x80] -add {RAMB18_X3Y384:RAMB18_X3Y395}
resize_pblock [get_pblocks pblock_rs_0x80] -add {RAMB36_X3Y192:RAMB36_X3Y197}

create_pblock pblock_core_pll
add_cells_to_pblock [get_pblocks pblock_core_pll] [get_cells -quiet [list ln_simple_i/core_pll]]
resize_pblock [get_pblocks pblock_core_pll] -add {CLOCKREGION_X7Y7:CLOCKREGION_X7Y7}

create_pblock pblock_noc_pll
add_cells_to_pblock [get_pblocks pblock_noc_pll] [get_cells -quiet [list ln_simple_i/noc_pll]]
resize_pblock [get_pblocks pblock_noc_pll] -add {CLOCKREGION_X7Y14:CLOCKREGION_X7Y14}

create_pblock pblock_vio
add_cells_to_pblock [get_pblocks pblock_vio] [get_cells -quiet [list ln_simple_i/vio_0]]
resize_pblock [get_pblocks pblock_vio] -add {CLOCKREGION_X0Y10:CLOCKREGION_X1Y10}

set_property C_CLK_INPUT_FREQ_HZ 300000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER false [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets clk]
