create_pblock pblock_mem
add_cells_to_pblock [get_pblocks pblock_mem] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/iowrp_west ln_simple_i/u_jtag_ddr_subsys]]
resize_pblock [get_pblocks pblock_mem] -add {CLOCKREGION_X0Y11:CLOCKREGION_X1Y14}
create_pblock pblock_hnf_1
add_cells_to_pblock [get_pblocks pblock_hnf_1] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_1 ln_simple_i/ln/inst/soc/uncore/noc/hnf_1_rst_sync]]
resize_pblock [get_pblocks pblock_hnf_1] -add {CLOCKREGION_X4Y15:CLOCKREGION_X8Y19 CLOCKREGION_X4Y14:CLOCKREGION_X7Y14}
create_pblock pblock_cc_0
add_cells_to_pblock [get_pblocks pblock_cc_0] [get_cells -quiet [list ln_simple_i/ln/inst/soc/cc_0 ln_simple_i/ln/inst/soc/uncore/noc/ccn_0_0x48 ln_simple_i/ln/inst/soc/uncore/noc/dev_reset_cc_0_rst_sync]]
resize_pblock [get_pblocks pblock_cc_0] -add {CLOCKREGION_X2Y11:CLOCKREGION_X8Y12 CLOCKREGION_X0Y5:CLOCKREGION_X8Y10}
create_pblock pblock_hnf_0
add_cells_to_pblock [get_pblocks pblock_hnf_0] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/hnf_0 ln_simple_i/ln/inst/soc/uncore/noc/hnf_0_rst_sync]]
resize_pblock [get_pblocks pblock_hnf_0] -add {CLOCKREGION_X0Y15:CLOCKREGION_X4Y19 CLOCKREGION_X1Y14:CLOCKREGION_X4Y14}
create_pblock pblock_rs_0x0
add_cells_to_pblock [get_pblocks pblock_rs_0x0] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_sn_id_0x0 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_sn_id_0x0_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x0] -add {SLICE_X99Y810:SLICE_X122Y839}
resize_pblock [get_pblocks pblock_rs_0x0] -add {RAMB18_X2Y324:RAMB18_X2Y335}
resize_pblock [get_pblocks pblock_rs_0x0] -add {RAMB36_X2Y162:RAMB36_X2Y167}
create_pblock pblock_rs_0x8
add_cells_to_pblock [get_pblocks pblock_rs_0x8] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x8 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_0_id_0x8_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x8] -add {SLICE_X137Y810:SLICE_X170Y839}
resize_pblock [get_pblocks pblock_rs_0x8] -add {DSP48E2_X2Y324:DSP48E2_X2Y335}
resize_pblock [get_pblocks pblock_rs_0x8] -add {RAMB18_X3Y324:RAMB18_X3Y335}
resize_pblock [get_pblocks pblock_rs_0x8] -add {RAMB36_X3Y162:RAMB36_X3Y167}
create_pblock pblock_rs_0x18
add_cells_to_pblock [get_pblocks pblock_rs_0x18] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x18 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x18_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x18] -add {SLICE_X235Y810:SLICE_X246Y839}
create_pblock pblock_rs_0x10
add_cells_to_pblock [get_pblocks pblock_rs_0x10] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x10 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x10_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x10] -add {SLICE_X195Y810:SLICE_X202Y839}
resize_pblock [get_pblocks pblock_rs_0x10] -add {DSP48E2_X3Y324:DSP48E2_X3Y335}
resize_pblock [get_pblocks pblock_rs_0x10] -add {RAMB18_X4Y324:RAMB18_X4Y335}
resize_pblock [get_pblocks pblock_rs_0x10] -add {RAMB36_X4Y162:RAMB36_X4Y167}
create_pblock pblock_rs_0x20
add_cells_to_pblock [get_pblocks pblock_rs_0x20] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x20 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hnf_1_id_0x20_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x20] -add {SLICE_X263Y810:SLICE_X296Y839}
resize_pblock [get_pblocks pblock_rs_0x20] -add {DSP48E2_X5Y324:DSP48E2_X5Y335}
resize_pblock [get_pblocks pblock_rs_0x20] -add {URAM288_X0Y216:URAM288_X0Y223}
create_pblock pblock_rs_0x28
add_cells_to_pblock [get_pblocks pblock_rs_0x28] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x28 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x28_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x28] -add {SLICE_X321Y810:SLICE_X334Y839}
create_pblock pblock_rs_0x30
add_cells_to_pblock [get_pblocks pblock_rs_0x30] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_rnh_id_0x30 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_rnh_id_0x30_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x30] -add {SLICE_X367Y810:SLICE_X397Y839}
resize_pblock [get_pblocks pblock_rs_0x30] -add {DSP48E2_X6Y324:DSP48E2_X6Y335}
create_pblock pblock_rs_0x38
add_cells_to_pblock [get_pblocks pblock_rs_0x38] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hni_id_0x38 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_hni_id_0x38_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x38] -add {SLICE_X367Y780:SLICE_X397Y809}
resize_pblock [get_pblocks pblock_rs_0x38] -add {DSP48E2_X6Y312:DSP48E2_X6Y323}
create_pblock pblock_rs_0x40
add_cells_to_pblock [get_pblocks pblock_rs_0x40] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x40 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x40_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x40] -add {SLICE_X321Y780:SLICE_X334Y809}
create_pblock pblock_rs_0x48
add_cells_to_pblock [get_pblocks pblock_rs_0x48] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_ccn_0_id_0x48 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_ccn_0_id_0x48_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x48] -add {SLICE_X263Y780:SLICE_X296Y809}
resize_pblock [get_pblocks pblock_rs_0x48] -add {DSP48E2_X5Y312:DSP48E2_X5Y323}
resize_pblock [get_pblocks pblock_rs_0x48] -add {URAM288_X0Y208:URAM288_X0Y215}
create_pblock pblock_rs_0x50
add_cells_to_pblock [get_pblocks pblock_rs_0x50] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x50 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x50_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x50] -add {SLICE_X235Y780:SLICE_X246Y809}
create_pblock pblock_rs_0x58
add_cells_to_pblock [get_pblocks pblock_rs_0x58] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x58 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x58_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x58] -add {SLICE_X195Y780:SLICE_X202Y809}
resize_pblock [get_pblocks pblock_rs_0x58] -add {DSP48E2_X3Y312:DSP48E2_X3Y323}
resize_pblock [get_pblocks pblock_rs_0x58] -add {RAMB18_X4Y312:RAMB18_X4Y323}
resize_pblock [get_pblocks pblock_rs_0x58] -add {RAMB36_X4Y156:RAMB36_X4Y161}
create_pblock pblock_rs_0x60
add_cells_to_pblock [get_pblocks pblock_rs_0x60] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x60 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_pip_id_0x60_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x60] -add {SLICE_X149Y780:SLICE_X156Y809}
resize_pblock [get_pblocks pblock_rs_0x60] -add {DSP48E2_X2Y312:DSP48E2_X2Y323}
resize_pblock [get_pblocks pblock_rs_0x60] -add {RAMB18_X3Y312:RAMB18_X3Y323}
resize_pblock [get_pblocks pblock_rs_0x60] -add {RAMB36_X3Y156:RAMB36_X3Y161}
create_pblock pblock_rs_0x68
add_cells_to_pblock [get_pblocks pblock_rs_0x68] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_mn_id_0x68 ln_simple_i/ln/inst/soc/uncore/noc/ring/ring_stop_mn_id_0x68_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x68] -add {SLICE_X99Y780:SLICE_X122Y809}
resize_pblock [get_pblocks pblock_rs_0x68] -add {RAMB18_X2Y312:RAMB18_X2Y323}
resize_pblock [get_pblocks pblock_rs_0x68] -add {RAMB36_X2Y156:RAMB36_X2Y161}
create_pblock pblock_peri
add_cells_to_pblock [get_pblocks pblock_peri] [get_cells -quiet [list ln_simple_i/ln/inst/soc/uncore/devWrp ln_simple_i/ln/inst/soc/uncore/dmaXBar ln_simple_i/ln/inst/soc/uncore/extMstCfgBuf ln_simple_i/ln/inst/soc/uncore/extSlvCfgBuf ln_simple_i/ln/inst/soc/uncore/noc/iowrp_east ln_simple_i/u_peri_subsys]]
resize_pblock [get_pblocks pblock_peri] -add {CLOCKREGION_X8Y13:CLOCKREGION_X8Y14}
set_property C_CLK_INPUT_FREQ_HZ 300000000 [get_debug_cores dbg_hub]
set_property C_ENABLE_CLK_DIVIDER false [get_debug_cores dbg_hub]
set_property C_USER_SCAN_CHAIN 1 [get_debug_cores dbg_hub]
connect_debug_port dbg_hub/clk [get_nets clk]
