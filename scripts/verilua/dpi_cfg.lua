dpi_exporter_config = {
    {
        module = "SimTop",
        signals = { "difftest_timer" },
        is_top_module = true
    },

    {
        module = "TL2CHICoupledL2",
        signals = {
            "clock",
            -- TODO: consider MMIOBridge
            "auto_in_.*(valid|ready|bits_address|bits_opcode|bits_param|bits_source|bits_data|bits_sink)",
            "io_chi.*_(valid|ready|bits_addr|bits_opcode|bits_size|bits_txnID|bits_srcID|bits_tgtID|bits_dataID|bits_dbID|bits_resp|bits_data|bits_be|bits_homeNID|bits_retToSrc|bits_fwdState|bits_fwdTxnID|bits_fwdNID)"
        }
    },

    {
        module = "AxiBridge",
        signals = {
            "axi_aw_.*(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache)",
            "axi_ar_.*(valid|ready|bits_id|bits_addr|bits_len|bits_size|bits_burst|bits_cache)",
            "axi_w_.*(valid|ready|bits_data|bits_strb|bits_last)",
            "axi_b_.*(valid|ready|bits_id|bits_resp)",
            "axi_r_.*(valid|ready|bits_id|bits_data|bits_resp|bits_last)"
        }
    }
}