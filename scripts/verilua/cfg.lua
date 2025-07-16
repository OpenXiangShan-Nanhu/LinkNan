local cfg = {}

cfg.mode = "step"
cfg.attach = true

cfg.simulator = assert(os.getenv("SIM"))

if cfg.simulator == "verilator" then
    cfg.top = "TOP.SimTop"
else -- vcs
    cfg.top = "tb_top"
end

local test_zhujiang_dir = "/nfs/share/home/zhengchuyu/TestZhuJiang"
local test_dongjiang_dir = "/nfs/share/home/zhengchuyu/TestDongJiang"

local path = require "pl.path"
if not path.isfile(path.join(test_zhujiang_dir, "xmake.lua")) then
    -- Use alternative path
    test_zhujiang_dir = "/nfs/share/zhengchuyu/TestZhuJiang"
end
if not path.isfile(path.join(test_dongjiang_dir, "xmake.lua")) then
    -- Use alternative path
    test_dongjiang_dir = "/nfs/share/zhengchuyu/TestDongJiang"
end
cfg.srcs = {
    "./?.lua",
    test_zhujiang_dir .. "/src/test/lua/common/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/v2/?.lua",
    test_dongjiang_dir .. "/src/?.lua"
}

do
	local soc_cfg
	local soc_cfg_file = assert(os.getenv("SOC_CFG_FILE"), "`SOC_CFG_FILE` is not set!")
	local soc_cfg_func = loadfile(soc_cfg_file)
	assert(type(soc_cfg_func) == "function", "`" .. soc_cfg_file .. "`" .. " is not exist or not a valid soc configuration file!")
	soc_cfg = soc_cfg_func()

	cfg.soc_cfg = soc_cfg
end

cfg.nr_l2 = #cfg.soc_cfg.l2c
cfg.nr_l2_slice = cfg.soc_cfg.l2c.nr_bank
cfg.nr_hnf = #cfg.soc_cfg.hnf
cfg.nr_sn = #cfg.soc_cfg.snf

cfg.enable_scoreboard_db = false
cfg.enable_scoreboard = true

cfg.enable_l2_mon_out = true
cfg.enable_l2_mon_in = true
cfg.enable_hnf_mon = true
cfg.enable_sn_mon = true

cfg.verbose_l2_mon_out = false
cfg.verbose_l2_mon_in = false
cfg.verbose_hnf_mon = false
cfg.verbose_sn_mon = false

return cfg