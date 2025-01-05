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
}

cfg.load_config_from_env = function ()
    local cfg = _G.cfg

    -- Check whether parameters are correctly set by enviroment variables
    local L2_CFG_STR = assert(os.getenv("L2_CFG_STR"), "Enviroment variable `L2_CFG_STR` is not set!")
    local DCU_NODE_STR = assert(os.getenv("DCU_NODE_STR"), "Enviroment variable `DCU_NODE_STR` is not set!")
    local NR_PCU = assert(os.getenv("NR_PCU"), "Enviroment variable `NR_PCU` is not set!")

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


    -- Parse DCU_NODE_STR
    local dcu_node_cfg = loadstring("return " .. DCU_NODE_STR)()
    cfg.dcu_node_cfg = dcu_node_cfg

    local nr_dj_dcu = 0
    for _, _ in pairs(dcu_node_cfg) do nr_dj_dcu = nr_dj_dcu + 1 end

    assert(type(dcu_node_cfg) == "table")
    assert(type(dcu_node_cfg[0]) == "table")
    cfg.nr_dj_dcu = nr_dj_dcu
    cfg.nr_dcu_port = #dcu_node_cfg[0]

    cfg.nr_dj_pcu = tonumber(NR_PCU)

    for i = 0, cfg.nr_dj_pcu - 1 do
        cfg["verbose_dj_pcu_mon_" .. i] = false
    
        cfg["enable_dj_pcu_mon_" .. i] = true
    end
    
    for i = 0, cfg.nr_dj_dcu - 1 do
        cfg["verbose_dj_dcu_mon_" .. i] = false
        cfg["enable_dj_dcu_mon_" .. i] = true
    end
end

cfg.enable_scoreboard = true

cfg.enable_l2_mon_out = true
cfg.enable_l2_mon_in = true
cfg.enable_dj_ddr_mon = true

cfg.verbose_l2_mon_out = false
cfg.verbose_l2_mon_in = false
cfg.verbose_dj_ddr_mon = false

return cfg