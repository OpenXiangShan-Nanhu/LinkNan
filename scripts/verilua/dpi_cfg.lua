local soc_cfg
local soc_cfg_file = assert(os.getenv("SOC_CFG_FILE"), "`SOC_CFG_FILE` is not set!")
local soc_cfg_func = loadfile(soc_cfg_file)
assert(type(soc_cfg_func) == "function", "`" .. soc_cfg_file .. "`" .. " is not exist or not a valid soc configuration file!")
soc_cfg = soc_cfg_func()

add_pattern {
    module = "SimTop",
    signals = "difftest_timer"
}

add_pattern {
    module = "TL2CHICoupledL2",
    inst = "soc.cc_0.tile.l2cache",
    signals = "(clock|reset)"
}

for _, chnl in ipairs({ "a", "b", "c", "d", "e" }) do
    local nr_bank = soc_cfg.l2c.nr_bank
    local signal_prefix_fmt = "auto_in_%d_"
    if nr_bank == 1 then
        signal_prefix_fmt = "auto_in_"
    end
    for bank = 0, nr_bank - 1 do
        for _, cfg in ipairs(soc_cfg.l2c) do
            local hier = cfg[1]:gsub("^SimTop%.", "")
            add_pattern {
                module = "TL2CHICoupledL2",
                inst = hier,
                sensitive_signals = ".*valid",
                signals = signal_prefix_fmt:format(bank) .. chnl .. "_(valid|ready|bits_address|bits_opcode|bits_param|bits_source|bits_data|bits_sink)"
            }
        end
    end
end

for _, chnl in ipairs({ "tx_req", "tx_rsp", "tx_dat", "rx_snp", "rx_rsp", "rx_dat" }) do
    for _, cfg in ipairs(soc_cfg.l2c) do
        local hier = cfg[1]:gsub("^SimTop%.", "")
        add_pattern {
            module = "TL2CHICoupledL2",
            inst = hier,
            sensitive_signals = ".*valid",
            signals = "io_chi_" .. chnl .. "_(valid|ready|bits_addr|bits_opcode|bits_size|bits_txnID|bits_srcID|bits_tgtID|bits_dataID|bits_dbID|bits_resp|bits_data|bits_be|bits_homeNID|bits_retToSrc|bits_fwdState|bits_fwdTxnID|bits_fwdNID)"
        }
    end
end

for _, chnl in ipairs({ "aw", "ar", "w", "b", "r" }) do
    for _, cfg in ipairs(soc_cfg.snf) do
        local hier = cfg[1]:gsub("^SimTop%.", "")
        add_pattern {
            module = "AxiBridge",
            inst = hier,
            sensitive_signals = ".*valid",
            signals = "axi_" .. chnl .. "_(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache|bits_data|bits_strb|bits_last|bits_resp)"
        }
    end
end

for _, chnl in ipairs({ "tx_req", "tx_resp", "tx_data", "tx_snoop", "rx_req", "rx_resp", "rx_data" }) do
    for _, cfg in ipairs(soc_cfg.hnf) do
        local hier = cfg[1]:gsub("^SimTop%.", "")
        add_pattern {
            module = "DongJiang",
            inst = hier,
            sensitive_signals = ".*valid",
            signals = "io_lan_" .. chnl .. "(?!.*(Excl|QoS|RespErr|CBusy|DoNotGoToSD|.*Attr)).*"
        }
    end
end

add_pattern {
    module = "HomeWrapper",
    signals = "erqTgt"
}
