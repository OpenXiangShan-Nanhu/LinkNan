dpi_exporter_config = {
    {
        module = "SimTop",
        signal = { "timer" },
        is_top_module = true
    },

    {
        module = "SimpleL2CacheDecoupled",
        signal = {
            "clock",
            "auto_sink_nodes_in_.*_(valid|ready|bits_address|bits_opcode|bits_param|bits_source|bits_data|bits_sink)",
            "io_chi.*_(valid|ready|bits_addr|bits_opcode|bits_txnID|bits_srcID|bits_tgtID|bits_dataID|bits_dbID|bits_resp|bits_data|bits_homeNID|bits_retToSrc|bits_fwdTxnID|bits_fwdNID)",
        }
    },

    {
        module = "ProtocolCtrlUnit",
        signal = {
            "io_toLocal_.*_(valid|ready|bits_Addr|bits_Opcode|bits_TxnID|bits_SrcID|bits_TgtID|bits_DBID|bits_DataID|bits_HomeNID|bits_ReturnTxnID|bits_ReturnNID|bits_FwdTxnID|bits_FwdNID|bits_Resp|bits_Data|bits_RetToSrc|bits_DbgAddr)",
        }
    },

    {
        module = "DataCtrlUnit",
        signal = {
            "io_icns_.*_(valid|ready|bits_Addr|bits_Opcode|bits_TxnID|bits_SrcID|bits_TgtID|bits_DBID|bits_DataID|bits_HomeNID|bits_ReturnTxnID|bits_ReturnNID|bits_Resp|bits_Data|bits_RetToSrc)",
        }
    },

    {
        module = "MemoryComplex",
        signal = {
            "io_icn_mem_.*_(valid|ready|bits_Addr|bits_Opcode|bits_TxnID|bits_SrcID|bits_TgtID|bits_DBID|bits_DataID|bits_HomeNID|bits_ReturnTxnID|bits_ReturnNID|bits_Resp|bits_Data|bits_RetToSrc)",
        }
    }
}