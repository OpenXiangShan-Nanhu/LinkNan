---@diagnostic disable: undefined-field, undefined-global

local LuaDataBase = require "LuaDataBase"
local L2TLMonitor do
    os.setenv("KMH", 1)
    L2TLMonitor = require "L2TLMonitorV2"
end
local L2CHIMonitor do
    os.setenv("L2CHIMonitorLCrd", 0)
    L2CHIMonitor = require "L2CHIMonitorV2"
end
local AXI4Monitor = require "AXI4Monitor"

local f = string.format
local tonumber = tonumber

local l2_mon_in_vec = {}
local l2_mon_out_vec = {}
local sn_mon_vec = {}

local function init_components()
    local tl_db = LuaDataBase({
        table_name = "tl_db",
        elements = {
            "cycles => INTEGER",
            "channel => TEXT",
            "opcode => TEXT",
            "param => TEXT",
            "address => TEXT",
            "source => INTEGER",
            "sink => INTEGER",
            "data => TEXT",
            "others => TEXT",
        },
        path = ".",
        file_name = "tl_db.db",
        save_cnt_max = 1000000 * 1,
        verbose = false,
    })

    local chi_db = LuaDataBase({
        table_name = "chi_db",
        elements = {
            "cycles => INTEGER",
            "channel => TEXT",
            "opcode => TEXT",
            "address => TEXT",
            "txn_id => INTEGER",
            "src_id => INTEGER",
            "tgt_id => INTEGER",
            "db_id => INTEGER",
            "resp => TEXT",
            "data => TEXT",
            "others => TEXT",
        },
        path = ".",
        file_name = "chi_db.db",
        save_cnt_max = 1000000 * 1,
        verbose = false,
    })

    for k, v in pairs(cfg.l2_cfg) do
        local l2_id = k
        local nr_slice = v[1]
        local nr_core = v[2] -- TODO: Not used for now
        
        local l2_prefix = ""
        local l2_hier = ""

        l2_hier = tostring(dut.soc["cc_" .. l2_id].tile.l2cache)

        local gen_l2_prefix = function (chnl, idx)
            if nr_slice == 1 then
                return "auto_in_" .. chnl .. "_"
            else
                return f("auto_in_%d_%s_", idx, chnl)
            end
        end

        for j = 0, nr_slice - 1 do
            local tl_a = ([[
                | valid
                | ready
                | bits_address => address
                | bits_opcode => opcode
                | bits_param => param
                | bits_source => source
            ]]):abdl({ hier = l2_hier, prefix = gen_l2_prefix("a", j), name = "L2 TL A" })

            local tl_b = ([[
                | valid
                | ready
                | bits_address => address
                | bits_opcode => opcode
                | bits_param => param
                | bits_data => data
            ]]):abdl({ hier = l2_hier, prefix = gen_l2_prefix("b", j), name = "L2 TL B" })
            tl_b.source = { __type = "CallableHDL", get = function () return 0 end }

            local tl_c = ([[
                | valid
                | ready
                | bits_address => address
                | bits_opcode => opcode
                | bits_param => param
                | bits_source => source
                | bits_data => data
            ]]):abdl({ hier = l2_hier, prefix = gen_l2_prefix("c", j), name = "L2 TL C" })

            local tl_d = ([[
                | valid
                | ready
                | bits_opcode => opcode
                | bits_param => param
                | bits_source => source
                | bits_data => data
                | bits_sink => sink
            ]]):abdl({ hier = l2_hier, prefix = gen_l2_prefix("d", j), name = "L2 TL D" })

            local tl_e = ([[
                | valid
                | bits_sink => sink
            ]]):abdl({ hier = l2_hier, prefix = gen_l2_prefix("e", j), name = "L2 TL E" })

            local l2_mon_in = L2TLMonitor(
                f("l2_mon_in_cluster_%d_slice_%d", l2_id, j), -- name

                --
                -- TileLink channels
                --
                tl_a,
                tl_b,
                tl_c,
                tl_d,
                tl_e,

                tl_db,
                cfg:get_or_else("verbose_l2_mon_in", true),
                cfg:get_or_else("enable_l2_mon_in", true)
            )

            table.insert(l2_mon_in_vec, l2_mon_in)
        end

        local txreq = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_tgtID => tgtID
            | bits_addr => addr
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_size => size
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_tx_req_", name = "L2 CHI TXREQ" })

        local txrsp = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_tgtID => tgtID
            | bits_dbID => dbID
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_resp => resp
            | bits_fwdState => fwdState
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_tx_rsp_", name = "L2 CHI TXRSP" })

        local txdat = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_tgtID => tgtID
            | bits_dbID => dbID
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_resp => resp
            | bits_data => data
            | bits_be => be
            | bits_dataID => dataID
            | bits_homeNID => homeNID
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_tx_dat_", name = "L2 CHI TXDAT" })

        local rxrsp = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_tgtID => tgtID
            | bits_dbID => dbID
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_resp => resp
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_rx_rsp_", name = "L2 CHI RXRSP" })

        local rxdat = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_tgtID => tgtID
            | bits_dbID => dbID
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_resp => resp
            | bits_data => data
            | bits_be => be
            | bits_dataID => dataID
            | bits_homeNID => homeNID
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_rx_dat_", name = "L2 CHI RXDAT" })

        local rxsnp = ([[
            | valid
            | ready
            | bits_srcID => srcID
            | bits_addr => addr
            | bits_opcode => opcode
            | bits_txnID => txnID
            | bits_retToSrc => retToSrc
            | bits_fwdTxnID => fwdTxnID
            | bits_fwdNID => fwdNID
        ]]):abdl({ hier = l2_hier, prefix = "io_chi_rx_snp_", name = "L2 CHI RXSNP" })

        local l2_mon_out = L2CHIMonitor(
            f("l2_mon_out_cluster_%d", l2_id), -- name
            -- l2_id, -- index
            --
            -- CHI Channels
            --
            txreq,
            txrsp,
            txdat,
            rxrsp,
            rxdat,
            rxsnp,

            chi_db,
            cfg:get_or_else("verbose_l2_mon_out", false),
            cfg:get_or_else("enable_l2_mon_out", true)
        )
        table.insert(l2_mon_out_vec, l2_mon_out)
    end

    do
        assert(cfg.nr_sn == 1, "TODO: nr_sn != 1")

        local sn_hier = tostring(dut.soc.uncore.noc.chi_to_axi_0x30)

        local axi_aw = ([[
            | valid
            | ready
            | bits_id => id
            | bits_addr => addr
            | bits_size => size
            | bits_len => len
            | bits_burst => burst
            | bits_cache => cache
        ]]):abdl {hier = sn_hier, prefix = "axi_aw_", name = "SN AXI AW"}

        local axi_ar = ([[
            | valid
            | ready
            | bits_id => id
            | bits_addr => addr
            | bits_size => size
            | bits_len => len
            | bits_burst => burst
            | bits_cache => cache
        ]]):abdl {hier = sn_hier, prefix = "axi_ar_", name = "SN AXI AR"}

        local axi_w = ([[
            | valid
            | ready
            | bits_data => data
            | bits_strb => strb
            | bits_last => last
        ]]):abdl {hier = sn_hier, prefix = "axi_w_", name = "SN AXI W"}
        axi_w.id = {
            __type = "CallableHDL",
            get = function ()
                return 0
            end
        }

        local axi_b = ([[
            | valid
            | ready
            | bits_id => id
            | bits_resp => resp
        ]]):abdl {hier = sn_hier, prefix = "axi_b_", name = "SN AXI B"}

        local axi_r = ([[
            | valid
            | ready
            | bits_id => id
            | bits_data => data
            | bits_resp => resp
            | bits_last => last
        ]]):abdl {hier = sn_hier, prefix = "axi_r_", name = "SN AXI R"}

        local sn_mon = AXI4Monitor(
            "sn_mon_out",
                
            -- 
            -- AXI Channels
            -- 
            axi_aw,
            axi_ar,
            axi_w,
            axi_b,
            axi_r,

            cfg:get_or_else("verbose_sn_mon", true),
            cfg:get_or_else("enable_sn_mon", true)
        )

        table.insert(sn_mon_vec, sn_mon)
    end
end

local print = function(...) print("[main.lua]", ...) end

fork {
    function ()
        local l2 = dut.soc.cc_0.tile.l2cache
        local clock = l2.clock:chdl()
        local timer = dut.difftest_timer:chdl()

        clock:posedge() do
            cfg.load_config_from_env()
            init_components()
        end
        
        local nr_l2_mon_in = #l2_mon_in_vec
        local nr_l2_mon_out = #l2_mon_out_vec
        local nr_sn_mon = #sn_mon_vec

        print("hello from main.lua")

        local cycles = tonumber(timer:get())
        while true do
            for i = 1, nr_l2_mon_in do
                l2_mon_in_vec[i]:sample_all(cycles)
            end

            for i = 1, nr_l2_mon_out do
                l2_mon_out_vec[i]:sample_all(cycles)
            end

            for i = 1, nr_sn_mon do
                sn_mon_vec[i]:sample_all(cycles)
            end

            cycles = tonumber(timer:get())
            clock:posedge()
        end
    end
}

verilua "finishTask" {
    function (is_error)
        -- if is_error and cfg.simulator == "verilator" then
        --     local symbol_helper = require "verilua.utils.SymbolHelper"
        --     local xs_assert = symbol_helper.ffi_cast("void (*)(long long)", "xs_assert")
            
        --     xs_assert(0)
        -- end
    end
}