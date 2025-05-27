add_pattern {
    module = "SimTop",
    signals = "difftest_timer"
}

add_pattern {
    module = "TL2CHICoupledL2",
    signals = "clock"
}

add_pattern {
    module = "TL2CHICoupledL2",
    signals = "auto_in_.*(valid|ready|bits_address|bits_opcode|bits_param|bits_source|bits_data|bits_sink)"
}

add_pattern {
    module = "TL2CHICoupledL2",
    signals = "io_chi.*_(valid|ready|bits_addr|bits_opcode|bits_size|bits_txnID|bits_srcID|bits_tgtID|bits_dataID|bits_dbID|bits_resp|bits_data|bits_be|bits_homeNID|bits_retToSrc|bits_fwdState|bits_fwdTxnID|bits_fwdNID)"
}

add_pattern {
    module = "AxiBridge",
    signals = "axi_aw_.*(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache)"
}

add_pattern {
    module = "AxiBridge",
    signals = "axi_ar_.*(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache)"
}

add_pattern {
    module = "AxiBridge",
    signals = "axi_w_.*(valid|ready|bits_data|bits_strb|bits_last)"
}

add_pattern {
    module = "AxiBridge",
    signals = "axi_b_.*(valid|ready|bits_id|bits_resp)"
}

add_pattern {
    module = "AxiBridge",
    signals = "axi_r_.*(valid|ready|bits_id|bits_data|bits_resp|bits_last)"
}

add_pattern {
    module = "DongJiang",
    signals = "io_lan_(tx_req|tx_resp|tx_data|tx_snoop|rx_req|rx_resp|rx_data)(?!.*(Excl|RespErr|CBusy|DoNotGoToSD|.*Attr)).*"
}

add_pattern {
    module = "HomeWrapper",
    signals = "erqTgt"
}