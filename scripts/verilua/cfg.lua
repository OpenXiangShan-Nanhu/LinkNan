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
cfg.srcs = {
    "./?.lua",
    test_zhujiang_dir .. "/src/test/lua/common/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/v2/?.lua",
    test_dongjiang_dir .. "/src/?.lua"
}

cfg.load_config_from_env = function ()
    local cfg = _G.cfg

    -- Check whether parameters are correctly set by enviroment variables
    local L2_CFG_STR = assert(os.getenv("L2_CFG_STR"), "Enviroment variable `L2_CFG_STR` is not set!")

    -- 
    -- l2_cfg:
    -- {
    --      [0] = { <l2_bank>, <l2_core> },
    --      [1] = ...
    --      ...
    -- }
    -- 
    local l2_cfg = loadstring("return " .. L2_CFG_STR)()
    cfg.l2_cfg = l2_cfg
end

do
	local soc_cfg
	local soc_cfg_file = assert(os.getenv("SOC_CFG_FILE"), "`SOC_CFG_FILE` is not set!")
	local soc_cfg_func = loadfile(soc_cfg_file)
	assert(type(soc_cfg_func) == "function", "`" .. soc_cfg_file .. "`" .. " is not exist or not a valid soc configuration file!")
	soc_cfg = soc_cfg_func()

	cfg.soc_cfg = soc_cfg
end

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