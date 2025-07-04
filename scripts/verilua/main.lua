---@diagnostic disable: undefined-field, undefined-global

local LuaDataBase = require "LuaDataBase"
local L2TLMonitor do
    os.setenv("KMH", 1)
    L2TLMonitor = require "L2TLMonitorV2"
end
local L2CHIMonitor = require "L2CHIMonitorV2"
local AXI4Monitor = require "AXI4Monitor"
local DJMonitor = require "DongJiangMonitor"

local f = string.format
local tonumber = tonumber

if cfg.simulator == "vcs" then
    _G.dut = _G.dut.sim
end

local l2_mon_in_vec = {}
local l2_mon_out_vec = {}
local hnf_mon_vec = {}
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

    local axi_db = LuaDataBase {
        table_name = "axi_db",
        elements = {
            "cycles => INTEGER",
            "channel => TEXT",
            "addr => TEXT",
            "id => INTEGER",
            "strb_hex_str => TEXT",
            "data_hex_str => TEXT",
            "others => TEXT",
        },
        save_cnt_max = 1000000 * 1,
        path = ".",
        file_name = "axi_db.db",
        verbose = false
    }

    local hnf_chi_db = LuaDataBase {
        table_name = "HNFChiDB",
        elements = {
            "cycles => INTEGER",
            "channel => TEXT",
            "opcode => TEXT",
            "address => TEXT",
            "txn_id => INTEGER",
            "dbid => INTEGER",
            "src_id => INTEGER",
            "tgt_id => INTEGER",
            "resp => TEXT",
            "data => TEXT",
            "others => TEXT",
        },
        path = ".",
        file_name = "hnf_chi_db.db",
        save_cnt_max = 1000000 * 1,
        verbose = false,
    }

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

    for i = 0, cfg.nr_sn - 1 do
        -- e.g.
        -- 	cfg.soc_cfg.snf = {
        -- 		{ "SimTop.soc.uncore.noc.chi_to_axi_0x20", { 32 } },
        -- 		{ "SimTop.soc.uncore.noc.chi_to_axi_0x48", { 72 } },
        -- 		{ "SimTop.soc.uncore.noc.chi_to_axi_0x50", { 80 } }
        -- 	}
        local sn_cfg = cfg.soc_cfg.snf[i + 1]
        local sn_hier = sn_cfg[1]:gsub("SimTop", cfg.top)
        if cfg.simulator == "vcs" then
            sn_hier = sn_cfg[1]:gsub("SimTop", dut:tostring())
        end

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
            "sn_mon_out_" .. i,

            -- 
            -- AXI Channels
            -- 
            axi_aw,
            axi_ar,
            axi_w,
            axi_b,
            axi_r,

            axi_db,

            cfg:get_or_else("verbose_sn_mon", true),
            cfg:get_or_else("enable_sn_mon", true),
            {
                scb_rd_update_wr_check = true
            }
        )

        table.insert(sn_mon_vec, sn_mon)
    end

    for i = 0, cfg.nr_hnf - 1 do
        local hnf_cfg = cfg.soc_cfg.hnf[i + 1]
        local hnf_hier = hnf_cfg[1]:gsub("SimTop", cfg.top)
        if cfg.simulator == "vcs" then
            hnf_hier = hnf_cfg[1]:gsub("SimTop", dut:tostring())
        end
        local dj_hier = hnf_hier .. ".hnx"

        local function make_fake_chdl()
            return {
                __type = "CallableHDL",
                get = function ()
                    return 5555
                end
            }
        end

        local chi_txreq = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_Opcode => opcode
            | bits_Size => size
            | bits_Addr => addr
            | bits_Order => order
            | bits_ExpCompAck => expCompAck
            | bits_ReturnTxnID => returnTxnID
            | bits_ReturnNID => returnNID
        ]]):abdl { hier = dj_hier, prefix = "io_lan_tx_req_", name = "chi_txreq for hnf_mon" }
        chi_txreq.memAttr = make_fake_chdl()
        chi_txreq.snpAttr = make_fake_chdl()

        local erqTgt = (hnf_hier .. ".erqTgt"):chdl()
        chi_txreq.tgtID = {
            __type = "CallableHDL",
            get = function ()
                return erqTgt:get()
            end
        }

        local chi_txrsp = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_Opcode => opcode
            | bits_Resp => resp
            | bits_FwdState => fwdState
            | bits_DBID => dbID
        ]]):abdl { hier = dj_hier, prefix = "io_lan_tx_resp_", name = "chi_txrsp for hnf_mon" }

        local chi_txdat = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_HomeNID => homeNID
            | bits_Opcode => opcode
            | bits_Resp => resp
            | bits_DBID => dbID
            | bits_DataSource => fwdState
            | bits_DataID => dataID
            | bits_BE => be
            | bits_Data => data
        ]]):abdl { hier = dj_hier, prefix = "io_lan_tx_data_", name = "chi_txdat for hnf_mon" }

        local chi_rxreq = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_Opcode => opcode
            | bits_Size => size
            | bits_Addr => addr
            | bits_Order => order
            | bits_ExpCompAck => expCompAck
        ]]):abdl { hier = dj_hier, prefix = "io_lan_rx_req_", name = "chi_rxreq for hnf_mon" }
        chi_rxreq.memAttr = make_fake_chdl()
        chi_rxreq.snpAttr = make_fake_chdl()

        local chi_rxrsp = ([[
            | valid
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_Opcode => opcode
            | bits_Resp => resp
            | bits_FwdState => fwdState
            | bits_DBID => dbID
        ]]):abdl { hier = dj_hier, prefix = "io_lan_rx_resp_", name = "chi_rxrsp for hnf_mon" }

        -- llc_rxchi_rxrsprsp.ready is optional, so when chi_rxrsp.ready is not exist, we need to add a fake ready signal
        local vpiml = require "vpiml"
        local dj_rxrsp_ready_hierpath = dj_hier .. ".io_lan_rx_resp_ready"
        local ret = vpiml.vpiml_handle_by_name_safe(dj_rxrsp_ready_hierpath)
        if ret == -1 then
            chi_rxrsp.ready = (""):fake_chdl({
                get = function (self)
                    return 1
                end
            })
        else
            chi_rxrsp.ready = dj_rxrsp_ready_hierpath:chdl()
        end

        local chi_rxdat = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_HomeNID => homeNID
            | bits_Opcode => opcode
            | bits_Resp => resp
            | bits_DBID => dbID
            | bits_DataID => dataID
            | bits_BE => be
            | bits_Data => data
        ]]):abdl { hier = dj_hier, prefix = "io_lan_rx_data_", name = "chi_rxdat for dj_mon" }

        local chi_txsnp = ([[
            | valid
            | ready
            | bits_TgtID => tgtID
            | bits_SrcID => srcID
            | bits_TxnID => txnID
            | bits_FwdNID => fwdNID
            | bits_FwdTxnID => fwdTxnID
            | bits_Opcode => opcode
            | bits_Addr => addr
            | bits_RetToSrc => retToSrc
        ]]):abdl { hier = dj_hier, prefix = "io_lan_tx_snoop_", name = "chi_rxsnp for hnf_mon" }

        local hnf_mon = DJMonitor(
            "hnf_mon_" .. i,

            chi_txreq,
            chi_txrsp,
            chi_txdat,
            chi_txsnp,
            chi_rxreq,
            chi_rxrsp,
            chi_rxdat,

            hnf_chi_db,
            cfg:get_or_else("verbose_hnf_mon", true),
            cfg:get_or_else("enable_hnf_mon", true)
        )

        final {
            function ()
                hnf_mon:list_tasks()
            end
        }

        table.insert(hnf_mon_vec, hnf_mon)
    end

    if cfg.enable_scoreboard_db then
        local scb = require "GlobalScoreboard"
        scb:enable_debug_db({
            get_cycles = function ()
                return l2_mon_in_vec[1].cycles
            end,
        })
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
        local nr_hnf_mon = #hnf_mon_vec
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

            for i = 1, nr_hnf_mon do
                hnf_mon_vec[i]:sample_all(cycles)
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
         if is_error and cfg.simulator == "verilator" then
             local symbol_helper = require "verilua.utils.SymbolHelper"
             local xs_assert = symbol_helper.ffi_cast("void (*)(long long)", "xs_assert")

             xs_assert(0)
         end
    end
}
