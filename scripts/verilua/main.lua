local LuaDataBase = require "LuaDataBase"
local L2TLMonitor = require "L2TLMonitor"
local L2CHIMonitor = require "L2CHIMonitor"
local PCUMonitor = require "PCUMonitor"
local DCUMonitor = require "DCUMonitor"
local DDRMonitor = require "DDRMonitor"
local dj_addrmap = require "DJAddrMap"

local f = string.format

local l2_mon_in_vec = {}
local l2_mon_out_vec = {}
local dj_pcu_mon_vec = {}
local dj_dcu_mon_vec = {}
local dj_ddr_mon = nil

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
    },
    path = ".",
    file_name = "chi_db.db",
    save_cnt_max = 1000000 * 1,
    verbose = false,
})

for i = 0, cfg.nr_l2 - 1 do
    local l2_hier = ""
    if i == 0 then
        l2_hier = tostring(dut.soc.cc.csu.l2cache)
    else
        l2_hier = tostring(dut.soc["cc_" .. i].csu.l2cache)
    end

    for j = 0, cfg.nr_l2_slice - 1 do
        local l2_prefix = ""

        if cfg.nr_l2_slice == 1 then
            l2_prefix = "auto_sink_nodes_in_a_"
        else
            l2_prefix = f("auto_sink_nodes_in_%d_a_", j)
        end
        local tl_a = ([[
            | valid
            | ready
            | bits_address => address
            | bits_opcode => opcode
            | bits_param => param
            | bits_source => source
        ]]):abdl({ hier = l2_hier, prefix = l2_prefix, name = "L2 TL A" })

        if cfg.nr_l2_slice == 1 then
            l2_prefix = "auto_sink_nodes_in_b_"
        else
            l2_prefix = f("auto_sink_nodes_in_%d_b_", j)
        end
        local tl_b = ([[
            | valid
            | ready
            | bits_address => address
            | bits_opcode => opcode
            | bits_param => param
            | bits_source => source
            | bits_data => data
        ]]):abdl({ hier = l2_hier, prefix = l2_prefix, name = "L2 TL B" })

        if cfg.nr_l2_slice == 1 then
            l2_prefix = "auto_sink_nodes_in_c_"
        else
            l2_prefix = f("auto_sink_nodes_in_%d_c_", j)
        end
        local tl_c = ([[
            | valid
            | ready
            | bits_address => address
            | bits_opcode => opcode
            | bits_param => param
            | bits_source => source
            | bits_data => data
        ]]):abdl({ hier = l2_hier, prefix = l2_prefix, name = "L2 TL C" })

        if cfg.nr_l2_slice == 1 then
            l2_prefix = "auto_sink_nodes_in_d_"
        else
            l2_prefix = f("auto_sink_nodes_in_%d_d_", j)
        end
        local tl_d = ([[
            | valid
            | ready
            | bits_opcode => opcode
            | bits_param => param
            | bits_source => source
            | bits_data => data
            | bits_sink => sink
        ]]):abdl({ hier = l2_hier, prefix = l2_prefix, name = "L2 TL D" })

        if cfg.nr_l2_slice == 1 then
            l2_prefix = "auto_sink_nodes_in_e_"
        else
            l2_prefix = f("auto_sink_nodes_in_%d_e_", j)
        end
        local tl_e = ([[
            | valid
            | bits_sink => sink
        ]]):abdl({ hier = l2_hier, prefix = l2_prefix, name = "L2 TL E" })

        local l2_mon_in = L2TLMonitor(
            "l2_mon_in_slice_" .. j, -- name

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
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_txreq_", name = "L2 CHI TXREQ" })

    local txrsp = ([[
        | valid
        | ready
        | bits_srcID => srcID
        | bits_tgtID => tgtID
        | bits_dbID => dbID
        | bits_opcode => opcode
        | bits_txnID => txnID
        | bits_resp => resp
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_txrsp_", name = "L2 CHI TXRSP" })

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
        | bits_dataID => dataID
        | bits_homeNID => homeNID
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_txdat_", name = "L2 CHI TXDAT" })

    local rxrsp = ([[
        | valid
        | ready
        | bits_srcID => srcID
        | bits_tgtID => tgtID
        | bits_dbID => dbID
        | bits_opcode => opcode
        | bits_txnID => txnID
        | bits_resp => resp
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_rxrsp_", name = "L2 CHI RXRSP" })

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
        | bits_dataID => dataID
        | bits_homeNID => homeNID
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_rxdat_", name = "L2 CHI RXDAT" })

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
    ]]):abdl({ hier = l2_hier, prefix = "io_chi_rxsnp_", name = "L2 CHI RXSNP" })

    local l2_mon_out = L2CHIMonitor(
        "l2_mon_out",
        0, -- index
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

for i = 0, cfg.nr_dj_pcu - 1 do
    local dj_hier = tostring(dut.soc.noc["pcu_" .. i])

	local rxreq = ([[
		| valid
		| ready
		| bits_Addr => addr
		| bits_Opcode => opcode
		| bits_TxnID => txnID
		| bits_SrcID => srcID
		| bits_TgtID => tgtID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_rx_req_", name = "PCU RXREQ" })

	local rxdat = ([[
		| valid
		| ready
		| bits_Opcode => opcode
		| bits_TxnID => txnID
		| bits_SrcID => srcID
		| bits_TgtID => tgtID
		| bits_DBID => dbID
		| bits_Data => data
		| bits_Resp => resp
		| bits_DataID => dataID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_rx_data_", name = "PCU RXDAT" })

	local rxrsp = ([[
		| valid
		| ready
		| bits_Opcode => opcode
		| bits_SrcID => srcID
		| bits_TxnID => txnID
		| bits_TgtID => tgtID
		| bits_Resp => resp
		| bits_DBID => dbID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_rx_resp_", name = "PCU RXRSP" })

	local txreq = ([[
		| valid
		| ready
		| bits_Addr => addr
		| bits_DbgAddr => dbgaddr
		| bits_Opcode => opcode
		| bits_TxnID => txnID
		| bits_SrcID => srcID
		| bits_TgtID => tgtID
		| bits_ReturnTxnID => returnTxnID
		| bits_ReturnNID => returnNID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_tx_req_", name = "PCU TXRTEQ" })

	local txdat = ([[
		| valid
		| ready
		| bits_Opcode => opcode
		| bits_TxnID => txnID
		| bits_SrcID => srcID
		| bits_TgtID => tgtID
		| bits_DBID => dbID
		| bits_Data => data
		| bits_Resp => resp 
		| bits_DataID => dataID
        | bits_HomeNID => homeNID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_tx_data_", name = "PCU TXDAT" })

	local txsnp = ([[
		| valid
		| ready
		| bits_Opcode => opcode
		| bits_Addr => addr
		| bits_SrcID => srcID
		| bits_TgtID => tgtID
		| bits_TxnID => txnID
		| bits_RetToSrc => retToSrc
        | bits_FwdTxnID => fwdTxnID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_tx_snoop_", name = "PCU TXSNP" })

	local txrsp = ([[
		| valid
		| ready
		| bits_Opcode => opcode
		| bits_SrcID => srcID
		| bits_TxnID => txnID
		| bits_TgtID => tgtID
		| bits_Resp => resp
		| bits_DBID => dbID
	]]):abdl({ hier = dj_hier, prefix = "io_toLocal_tx_resp_", name = "PCU TXRSP" })

	local dj_pcu_mon = PCUMonitor(
		"dj_pcu_mon_" .. i,
		i,

		--
		-- CHI channels
		--
		rxreq,
		rxrsp,
		rxdat,
		txreq,
		txrsp,
		txdat,
		txsnp,

		chi_db,
		cfg:get_or_else(f("verbose_dj_pcu_mon_%d", i), false),
		cfg:get_or_else(f("enable_dj_pcu_mon_%d", i), true)
	)

	table.insert(dj_pcu_mon_vec, dj_pcu_mon)
end

for i = 0, cfg.nr_dj_dcu - 1 do
    local dj_hier = tostring(dut.soc.noc["dcu_" .. i])

	local rxreqs = {}
	local rxdats = {}
	local txdats = {}
	local txrsps = {}
	for j = 0, cfg.nr_dcu_port - 1 do
		local rxreq = ([[
			| valid
			| ready
			| bits_Addr => addr
			| bits_Opcode => opcode
			| bits_TxnID => txnID
			| bits_SrcID => srcID
			| bits_TgtID => tgtID
			| bits_ReturnNID => returnNID
			| bits_ReturnTxnID => returnTxnID
		]]):abdl({ hier = dj_hier, prefix = "io_icns_" .. j .. "_rx_req_", name = "DCU PORT" .. j .. " RXREQ" })

		local rxdat = ([[
			| valid
			| ready
			| bits_Opcode => opcode
			| bits_TxnID => txnID
			| bits_SrcID => srcID
			| bits_TgtID => tgtID
			| bits_DBID => dbID
			| bits_Data => data
			| bits_Resp => resp
			| bits_DataID => dataID
		]]):abdl({ hier = dj_hier, prefix = "io_icns_" .. j .. "_rx_data_", name = "DCU PORT" .. j .. " RXDAT" })

		local txdat = ([[
			| valid
			| ready
			| bits_Opcode => opcode
			| bits_TxnID => txnID
			| bits_SrcID => srcID
			| bits_TgtID => tgtID
			| bits_DBID => dbID
			| bits_Data => data
			| bits_Resp => resp 
			| bits_DataID => dataID 
		]]):abdl({ hier = dj_hier, prefix = "io_icns_" .. j .. "_tx_data_", name = "DCU PORT" .. j .. " TXDAT" })

		local txrsp = ([[
			| valid
			| ready
			| bits_Opcode => opcode
			| bits_SrcID => srcID
			| bits_TxnID => txnID
			| bits_TgtID => tgtID
			| bits_Resp => resp
			| bits_DBID => dbID
		]]):abdl({ hier = dj_hier, prefix = "io_icns_" .. j .. "_tx_resp_", name = "DCU PORT" .. j .. " TXRSP" })

		table.insert(rxreqs, rxreq)
		table.insert(rxdats, rxdat)
		table.insert(txdats, txdat)
		table.insert(txrsps, txrsp)
	end

	local dj_dcu_mon = DCUMonitor(
		"dj_dcu_mon_" .. i,
		i,

		--
		-- CHI channels
		--
		rxreqs,
		rxdats,
		txrsps,
		txdats,

		chi_db,
		cfg:get_or_else(f("verbose_dj_dcu_mon_%d", i), false),
		cfg:get_or_else(f("enable_dj_dcu_mon_%d", i), true),
		cfg.nr_dcu_port
	)

	table.insert(dj_dcu_mon_vec, dj_dcu_mon)
end


do
    local dj_ddr_hier = tostring(dut.soc.noc.memSubSys)

    local rxreq = ([[
        | valid
        | ready
        | bits_Addr => addr
        | bits_Opcode => opcode
        | bits_TxnID => txnID
        | bits_SrcID => srcID
        | bits_TgtID => tgtID
        | bits_ReturnNID => returnNID
        | bits_ReturnTxnID => returnTxnID
    ]]):abdl({ hier = dj_ddr_hier, prefix = "io_icn_mem_rx_req_", name = "DDR RXREQ" })
    
    local rxdat = ([[
        | valid
        | bits_Opcode => opcode
        | bits_TxnID => txnID
        | bits_SrcID => srcID
        | bits_TgtID => tgtID
        | bits_DBID => dbID
        | bits_Data => data
        | bits_Resp => resp
        | bits_DataID => dataID
    ]]):abdl({ hier = dj_ddr_hier, prefix = "io_icn_mem_rx_data_", name = "DDR RXDAT" })
    
    local txrsp = ([[
        | valid
        | ready
        | bits_Opcode => opcode
        | bits_SrcID => srcID
        | bits_TxnID => txnID
        | bits_TgtID => tgtID
        | bits_Resp => resp
        | bits_DBID => dbID
    ]]):abdl({ hier = dj_ddr_hier, prefix = "io_icn_mem_tx_resp_", name = "DDR TXRSP" })
    
    local txdat = ([[
        | valid
        | ready
        | bits_Opcode => opcode
        | bits_TxnID => txnID
        | bits_SrcID => srcID
        | bits_TgtID => tgtID
        | bits_DBID => dbID
        | bits_Data => data
        | bits_Resp => resp
        | bits_DataID => dataID
    ]]):abdl({ hier = dj_ddr_hier, prefix = "io_icn_mem_tx_data_", name = "DDR TXDAT" })
    

    dj_ddr_mon = DDRMonitor(
        "dj_ddr_mon",
    
        --
        -- CHI channels
        --
        rxreq,
        rxdat,
        txrsp,
        txdat,
    
        chi_db,
        cfg:get_or_else("verbose_dj_ddr_mon", false),
        cfg:get_or_else("enable_dj_ddr_mon", true)
    )
end

for dcu_idx, node_ids in pairs(cfg.dcu_node_cfg) do
    for dcu_port_idx, node_id in ipairs(node_ids) do
        dj_addrmap:update_dcu_nodeid(dcu_idx, dcu_port_idx - 1, node_id)
    end
end

local print = function(...) print("[main.lua]", ...) end

fork {
    function ()
        local l2 = dut.soc.cc.csu.l2cache
        local clock = l2.clock:chdl()
        local timer = dut.timer:chdl()
        
        local nr_l2_mon_in = #l2_mon_in_vec
        local nr_l2_mon_out = #l2_mon_out_vec
        local nr_dj_pcu_mon = #dj_pcu_mon_vec
        local nr_dj_dcu_mon = #dj_dcu_mon_vec

        print("hello from main.lua")

        local cycles = timer:get()
        while true do
            for i = 1, nr_l2_mon_in do
                l2_mon_in_vec[i]:sample_all(cycles)
            end

            for i = 1, nr_l2_mon_out do
                l2_mon_out_vec[i]:sample_all(cycles)
            end

            for i = 1, nr_dj_pcu_mon do
                dj_pcu_mon_vec[i]:sample_all(cycles)
            end

            for i = 1, nr_dj_dcu_mon do
                dj_dcu_mon_vec[i]:sample_all(cycles)
            end

            dj_ddr_mon:sample_all(cycles)

            cycles = timer:get()
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