add_pattern {
    module = "SimTop",
    signals = "difftest_timer"
}

add_pattern {
    module = "TL2CHICoupledL2",
    signals = "clock"
}

for _, chnl in ipairs({ "a", "b", "c", "d", "e" }) do
    add_pattern {
        module = "TL2CHICoupledL2",
        sensitive_signals = ".*valid",
        signals = "auto_in_.*_" .. chnl .. "_(valid|ready|bits_address|bits_opcode|bits_param|bits_source|bits_data|bits_sink)"
    }
end

for _, chnl in ipairs({ "tx_req", "tx_rsp", "tx_dat", "rx_snp", "rx_rsp", "rx_dat" }) do
    add_pattern {
        module = "TL2CHICoupledL2",
        sensitive_signals = ".*valid",
        signals = "io_chi_" .. chnl .. "_(valid|ready|bits_addr|bits_opcode|bits_size|bits_txnID|bits_srcID|bits_tgtID|bits_dataID|bits_dbID|bits_resp|bits_data|bits_be|bits_homeNID|bits_retToSrc|bits_fwdState|bits_fwdTxnID|bits_fwdNID)"
    }
end

for _, chnl in ipairs({ "aw", "ar", "w", "b", "r" }) do
    add_pattern {
        module = "AxiBridge",
        sensitive_signals = ".*valid",
        signals = "axi_" .. chnl .. "_(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache|bits_data|bits_strb|bits_last|bits_resp)"
    }
end

for _, chnl in ipairs({ "tx_req", "tx_resp", "tx_data", "tx_snoop", "rx_req", "rx_resp", "rx_data" }) do
    add_pattern {
        module = "DongJiang",
        sensitive_signals = ".*valid",
        signals = "io_lan_" .. chnl .. "(?!.*(Excl|QoS|RespErr|CBusy|DoNotGoToSD|.*Attr)).*"
    }
end

add_pattern {
    module = "HomeWrapper",
    signals = "erqTgt"
}
