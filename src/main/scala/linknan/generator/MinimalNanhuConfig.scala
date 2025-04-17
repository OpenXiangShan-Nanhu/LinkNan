package linknan.generator

import org.chipsalliance.cde.config.Config
import xiangshan.XSCoreParamsKey
import xiangshan.backend.dispatch.DispatchParameters
import xiangshan.backend.regfile.{IntPregParams, VfPregParams}
import xiangshan.cache.DCacheParameters
import xiangshan.cache.mmu.{L2TLBParameters, TLBParameters}
import xiangshan.frontend.icache.ICacheParameters

class MinimalNanhuConfig extends Config((site, here, up) => {
  case XSCoreParamsKey => up(XSCoreParamsKey).copy(
    DecodeWidth = 6,
    RenameWidth = 6,
    RobCommitWidth = 8,
    FetchWidth = 4,
    VirtualLoadQueueSize = 24,
    LoadQueueRAWSize = 12,
    LoadQueueReplaySize = 24,
    LoadUncacheBufferSize = 8,
    LoadQueueNWriteBanks = 4, // NOTE: make sure that LoadQueue{RAR, RAW, Replay}Size is divided by LoadQueueNWriteBanks.
    RollbackGroupSize = 8,
    StoreQueueSize = 20,
    StoreQueueNWriteBanks = 4, // NOTE: make sure that StoreQueueSize is divided by StoreQueueNWriteBanks
    StoreQueueForwardWithMask = true,
    // ============ VLSU ============
    VlMergeBufferSize = 16,
    VsMergeBufferSize = 8,
    UopWritebackWidth = 2,
    // ==============================
    RobSize = 48,
    RabSize = 96,
    FtqSize = 8,
    IBufSize = 24,
    IBufNBank = 6,
    StoreBufferSize = 4,
    StoreBufferThreshold = 3,
    dpParams = DispatchParameters(
      IntDqSize = 12,
      FpDqSize = 12,
      LsDqSize = 12,
      IntDqDeqWidth = 8,
      FpDqDeqWidth = 6,
      VecDqDeqWidth = 6,
      LsDqDeqWidth = 6
    ),
    intPreg = IntPregParams(
      numEntries = 64,
      numRead = None,
      numWrite = None,
    ),
    vfPreg = VfPregParams(
      numEntries = 128,
      numRead = None,
      numWrite = None,
    ),
    icacheParameters = ICacheParameters(
      nSets = 64, // 16KB ICache
      tagECC = Some("parity"),
      dataECC = Some("parity"),
      replacer = Some("setplru"),
    ),
    dcacheParametersOpt = Some(DCacheParameters(
      nSets = 256, // 2 bits alias
      nWays = 2,
      tagECC = Some("secded"),
      dataECC = Some("secded"),
      replacer = Some("setplru"),
      nMissEntries = 4,
      nProbeEntries = 4,
      nReleaseEntries = 4,
      nMaxPrefetchEntry = 2,
    )),
    // ============ BPU ===============
    EnableGHistDiff = false,
    FtbSize = 256,
    FtbWays = 2,
    RasSize = 8,
    RasSpecSize = 16,
    TageTableInfos =
      Seq((512, 4, 6),
        (512, 9, 6),
        (1024, 19, 6)),
    SCNRows = 128,
    SCNTables = 2,
    SCHistLens = Seq(0, 5),
    ITTageTableInfos =
      Seq((256, 4, 7),
        (256, 8, 7),
        (512, 16, 7)),
    // ================================
    itlbParameters = TLBParameters(
      name = "itlb",
      fetchi = true,
      useDmode = false,
      NWays = 4,
    ),
    dtlbParameters = TLBParameters(
      name = "ldtlb",
      NWays = 4,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      outReplace = false,
      lgMaxSize = 4
    ),
    ldtlbParameters = TLBParameters(
      name = "ldtlb",
      NWays = 4,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      outReplace = false,
      lgMaxSize = 4
    ),
    sttlbParameters = TLBParameters(
      name = "sttlb",
      NWays = 4,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      outReplace = false,
      lgMaxSize = 4
    ),
    hytlbParameters = TLBParameters(
      name = "hytlb",
      NWays = 4,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      outReplace = false,
      lgMaxSize = 4
    ),
    pftlbParameters = TLBParameters(
      name = "pftlb",
      NWays = 4,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      outReplace = false,
      lgMaxSize = 4
    ),
    btlbParameters = TLBParameters(
      name = "btlb",
      NWays = 4,
    ),
    l2tlbParameters = L2TLBParameters(
      l3Size = 4,
      l2Size = 4,
      l1nSets = 4,
      l1nWays = 4,
      l1ReservedBits = 1,
      l0nSets = 4,
      l0nWays = 8,
      l0ReservedBits = 0,
      spSize = 4,
    )
  )
})
