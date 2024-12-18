local cfg = {}

cfg.mode = "step"
cfg.attach = true
cfg.top = "TOP.SimTop"

local test_zhujiang_dir = "/nfs/share/home/zhengchuyu/TestZhuJiang"
cfg.srcs = {
    "./?.lua",
    test_zhujiang_dir .. "/src/test/lua/common/?.lua",
    test_zhujiang_dir .. "/src/test/lua/component/?.lua",
}


-- Check whether parameters are correctly set by enviroment variables
local NR_L2 = assert(os.getenv("NR_L2"), "Enviroment variable `NR_L2` is not set!")
local NR_L2_BANK = assert(os.getenv("NR_L2_BANK"), "Enviroment variable `NR_L2_BANK` is not set!")
local NR_PCU = assert(os.getenv("NR_PCU"), "Enviroment variable `NR_PCU` is not set!")
local NR_DCU = assert(os.getenv("NR_DCU"), "Enviroment variable `NR_DCU` is not set!")
local DCU_NODE_STR = assert(os.getenv("DCU_NODE_STR"), "Enviroment variable `DCU_NODE_STR` is not set!")

cfg.nr_l2 = tonumber(NR_L2)
cfg.nr_l2_slice = tonumber(NR_L2_BANK)
cfg.nr_dj_pcu = tonumber(NR_PCU)
cfg.nr_dj_dcu = tonumber(NR_DCU)

-- Parse DCU_NODE_STR
local dcu_node_cfg = loadstring("return " .. DCU_NODE_STR)()
local nr_dj_dcu = 0
for _, _ in pairs(dcu_node_cfg) do nr_dj_dcu = nr_dj_dcu + 1 end

assert(type(dcu_node_cfg) == "table")
assert(type(dcu_node_cfg[0]) == "table")
assert(cfg.nr_dj_dcu == nr_dj_dcu)

cfg.nr_dcu_port = #dcu_node_cfg[0]
cfg.dcu_node_cfg = dcu_node_cfg

cfg.enable_l2_mon_out = true
cfg.enable_l2_mon_in = true
cfg.enable_dj_ddr_mon = true

cfg.verbose_l2_mon_out = false
cfg.verbose_l2_mon_in = false
cfg.verbose_dj_ddr_mon = false

for i = 0, cfg.nr_dj_pcu - 1 do
    cfg["verbose_dj_pcu_mon_" .. i] = false

    cfg["enable_dj_pcu_mon_" .. i] = true
end

for i = 0, cfg.nr_dj_dcu - 1 do
    cfg["verbose_dj_dcu_mon_" .. i] = false
    cfg["enable_dj_dcu_mon_" .. i] = true
end

return cfg