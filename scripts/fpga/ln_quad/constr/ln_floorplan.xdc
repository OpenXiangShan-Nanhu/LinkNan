
# LLC Placements
create_pblock pblock_hnf_0
add_cells_to_pblock [get_pblocks pblock_hnf_0] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_0]]
resize_pblock [get_pblocks pblock_hnf_0] -add {CLOCKREGION_S1X7Y6:CLOCKREGION_S1X8Y5}
create_pblock pblock_hnf_1
add_cells_to_pblock [get_pblocks pblock_hnf_1] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_1]]
resize_pblock [get_pblocks pblock_hnf_1] -add {CLOCKREGION_S2X8Y6:CLOCKREGION_S2X7Y5}
create_pblock pblock_hnf_2
add_cells_to_pblock [get_pblocks pblock_hnf_2] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_2]]
resize_pblock [get_pblocks pblock_hnf_2] -add {CLOCKREGION_S3X8Y5:CLOCKREGION_S3X7Y6}
create_pblock pblock_hnf_3
add_cells_to_pblock [get_pblocks pblock_hnf_3] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_3]]
resize_pblock [get_pblocks pblock_hnf_3] -add {CLOCKREGION_S0X7Y5:CLOCKREGION_S0X8Y6}

# CPU placements
create_pblock pblock_cc_0
add_cells_to_pblock [get_pblocks pblock_cc_0] [get_cells -quiet [list ln_quad_i/ln/inst/soc/cc_0]]
resize_pblock [get_pblocks pblock_cc_0] -add {CLOCKREGION_S1X0Y4:CLOCKREGION_S1X8Y1 CLOCKREGION_S1X0Y6:CLOCKREGION_S1X6Y5}
create_pblock pblock_cc_1
add_cells_to_pblock [get_pblocks pblock_cc_1] [get_cells -quiet [list ln_quad_i/ln/inst/soc/cc_1]]
resize_pblock [get_pblocks pblock_cc_1] -add {CLOCKREGION_S2X11Y0:CLOCKREGION_S2X0Y0 CLOCKREGION_S2X8Y3:CLOCKREGION_S2X0Y1 CLOCKREGION_S2X6Y6:CLOCKREGION_S2X0Y4}
create_pblock pblock_cc_2
add_cells_to_pblock [get_pblocks pblock_cc_2] [get_cells -quiet [list ln_quad_i/ln/inst/soc/cc_2]]
resize_pblock [get_pblocks pblock_cc_2] -add {CLOCKREGION_S3X6Y4:CLOCKREGION_S3X0Y6 CLOCKREGION_S3X8Y1:CLOCKREGION_S3X0Y3 CLOCKREGION_S3X11Y0:CLOCKREGION_S3X0Y0}
create_pblock pblock_cc_3
add_cells_to_pblock [get_pblocks pblock_cc_3] [get_cells -quiet [list ln_quad_i/ln/inst/soc/cc_3]]
resize_pblock [get_pblocks pblock_cc_3] -add {CLOCKREGION_S0X0Y5:CLOCKREGION_S0X6Y6 CLOCKREGION_S0X0Y1:CLOCKREGION_S0X8Y4 CLOCKREGION_S0X0Y0:CLOCKREGION_S0X11Y0}
create_pblock pblock_iowrp_dev
add_cells_to_pblock [get_pblocks pblock_iowrp_dev] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/devWrp ln_quad_i/ln/inst/soc/uncore/inst/maxiFabric ln_quad_i/ln/inst/soc/uncore/inst/noc/iowrp_dev ln_quad_i/ln/inst/soc/uncore/inst/sAxiFabric ln_quad_i/u_peri_subsys]]
resize_pblock [get_pblocks pblock_iowrp_dev] -add {CLOCKREGION_S3X8Y4:CLOCKREGION_S3X7Y4}
create_pblock pblock_iowrp_mem
add_cells_to_pblock [get_pblocks pblock_iowrp_mem] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/iowrp_mem]]
resize_pblock [get_pblocks pblock_iowrp_mem] -add {CLOCKREGION_S2X8Y4:CLOCKREGION_S2X7Y4}

# RS placements
create_pblock pblock_rs_0x00
add_cells_to_pblock [get_pblocks pblock_rs_0x00] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x0 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x0_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x00] -add {SLICE_S1X356Y476:SLICE_S1X415Y523}
create_pblock pblock_rs_0x08
add_cells_to_pblock [get_pblocks pblock_rs_0x08] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ccn_0_0x8 ln_quad_i/ln/inst/soc/uncore/inst/noc/dev_reset_cc_0_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_0_id_0x8 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_0_id_0x8_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x08] -add {SLICE_S1X356Y380:SLICE_S1X415Y426}
create_pblock pblock_rs_0x10
add_cells_to_pblock [get_pblocks pblock_rs_0x10] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x10 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x10_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x10] -add {SLICE_S1X416Y380:SLICE_S1X467Y427}
create_pblock pblock_rs_0x18
add_cells_to_pblock [get_pblocks pblock_rs_0x18] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_0_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_0_id_0x18 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_0_id_0x18_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x18] -add {SLICE_S1X416Y476:SLICE_S1X467Y523}
create_pblock pblock_rs_0x20
add_cells_to_pblock [get_pblocks pblock_rs_0x20] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_1_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_1_id_0x20 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_1_id_0x20_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x20] -add {SLICE_S2X416Y476:SLICE_S2X467Y523}
create_pblock pblock_rs_0x28_0x30
add_cells_to_pblock [get_pblocks pblock_rs_0x28_0x30] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_mn_id_0x28 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_mn_id_0x28_reset_resetSync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_sn_id_0x30 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_sn_id_0x30_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x28_0x30] -add {SLICE_S2X416Y380:SLICE_S2X467Y427}
create_pblock pblock_rs_0x38
add_cells_to_pblock [get_pblocks pblock_rs_0x38] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ccn_1_0x38 ln_quad_i/ln/inst/soc/uncore/inst/noc/dev_reset_cc_1_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_1_id_0x38 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_1_id_0x38_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x38] -add {SLICE_S2X356Y380:SLICE_S2X415Y427}
create_pblock pblock_rs_0x40
add_cells_to_pblock [get_pblocks pblock_rs_0x40] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x40 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x40_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x40] -add {SLICE_S2X356Y476:SLICE_S2X415Y523}
create_pblock pblock_rs_0x48
add_cells_to_pblock [get_pblocks pblock_rs_0x48] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x48 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x48_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x48] -add {SLICE_S3X356Y476:SLICE_S3X415Y523}
create_pblock pblock_rs_0x50
add_cells_to_pblock [get_pblocks pblock_rs_0x50] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ccn_2_0x50 ln_quad_i/ln/inst/soc/uncore/inst/noc/dev_reset_cc_2_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_2_id_0x50 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_2_id_0x50_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x50] -add {SLICE_S3X356Y380:SLICE_S3X415Y427}
create_pblock pblock_rs_0x58_0x60
add_cells_to_pblock [get_pblocks pblock_rs_0x58_0x60] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hni_id_0x60 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hni_id_0x60_reset_resetSync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_rni_id_0x58 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_rni_id_0x58_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x58_0x60] -add {SLICE_S3X416Y380:SLICE_S3X467Y427}
create_pblock pblock_rs_0x68
add_cells_to_pblock [get_pblocks pblock_rs_0x68] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_2_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_2_id_0x68 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_2_id_0x68_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x68] -add {SLICE_S3X416Y476:SLICE_S3X467Y522}
create_pblock pblock_rs_0x70
add_cells_to_pblock [get_pblocks pblock_rs_0x70] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/hnf_3_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_3_id_0x70 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_hnf_3_id_0x70_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x70] -add {SLICE_S0X416Y476:SLICE_S0X467Y523}
create_pblock pblock_rs_0x78
add_cells_to_pblock [get_pblocks pblock_rs_0x78] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x78 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x78_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x78] -add {SLICE_S0X416Y380:SLICE_S0X467Y427}
create_pblock pblock_rs_0x80
add_cells_to_pblock [get_pblocks pblock_rs_0x80] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ccn_3_0x80 ln_quad_i/ln/inst/soc/uncore/inst/noc/dev_reset_cc_3_rst_sync ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_3_id_0x80 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_ccn_3_id_0x80_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x80] -add {SLICE_S0X356Y379:SLICE_S0X415Y427}
create_pblock pblock_rs_0x88
add_cells_to_pblock [get_pblocks pblock_rs_0x88] [get_cells -quiet [list ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x88 ln_quad_i/ln/inst/soc/uncore/inst/noc/ring/ring_stop_pip_id_0x88_reset_resetSync]]
resize_pblock [get_pblocks pblock_rs_0x88] -add {SLICE_S0X356Y476:SLICE_S0X415Y523}

# MMCM Placements
create_pblock pblock_in_mmcm
add_cells_to_pblock [get_pblocks pblock_in_mmcm] [get_cells -quiet [list ln_quad_i/in_mmcm]]
resize_pblock [get_pblocks pblock_in_mmcm] -add {CLOCKREGION_S3X11Y0:CLOCKREGION_S3X11Y0}
