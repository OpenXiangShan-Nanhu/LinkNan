local cfg = {}

cfg.mode = "step"
cfg.attach = true
cfg.top = "TOP.SimTop"

local test_zhujiang_dir = "/nfs/home/zhengchuyu/workspace/noc/TestZhuJiang"
cfg.srcs = {
    "./?.lua",
    test_zhujiang_dir .. "/src/test/lua/common/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/?.lua",
}


cfg.nr_l2 = tonumber(os.getenv("NR_L2")) or 1
cfg.nr_l2_slice = tonumber(os.getenv("NR_L2_BANK")) or 2

-- Not used now
-- cfg.nr_dj_dcu = 2
-- cfg.nr_dj_pcu = 1

cfg.enable_l2_mon_out = true
cfg.enable_l2_mon_in = true

cfg.verbose_l2_mon_out = false
cfg.verbose_l2_mon_in = false

return cfg