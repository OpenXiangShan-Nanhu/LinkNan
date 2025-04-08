local cfg = {}

cfg.mode = "step"
cfg.attach = true

cfg.simulator = assert(os.getenv("SIM"))

if cfg.simulator == "verilator" then
    cfg.top = "TOP.SimTop"
else
    cfg.top = "tb_top.sim"
end

local test_zhujiang_dir = "/nfs/share/home/zhengchuyu/TestZhuJiang"
cfg.srcs = {
    "./?.lua",
    test_zhujiang_dir .. "/src/test/lua/common/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/v2/?.lua",
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


cfg.nr_sn = 1

cfg.enable_scoreboard = true

cfg.enable_l2_mon_out = true
cfg.enable_l2_mon_in = true
cfg.enable_sn_mon = true

cfg.verbose_l2_mon_out = true
cfg.verbose_l2_mon_in = true
cfg.verbose_sn_mon = true

return cfg