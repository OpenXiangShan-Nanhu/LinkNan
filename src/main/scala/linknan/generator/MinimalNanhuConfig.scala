package linknan.generator

import org.chipsalliance.cde.config.Config
import xiangshan.XSCoreParamsKey
import xiangshan.backend.regfile.{IntPregParams, V0PregParams, VfPregParams, VlPregParams}
import xiangshan.cache.DCacheParameters
import xiangshan.frontend.icache.ICacheParameters
import xiangshan.backend.dispatch.DispatchParameters
import xiangshan.cache.mmu.{TLBParameters, L2TLBParameters}

class MinimalNanhuConfig extends Config((site, here, up) => {
  case XSCoreParamsKey => up(XSCoreParamsKey).copy(
    DecodeWidth = 2,      //2
    RenameWidth = 2,      //4
    IBufSize = 16,        //32
    FtqSize = 16,         //48
    FtbSize = 2048,
    //Backend
    RobSize = 48,         //96
    RabSize = 96,         //96
    intPreg = IntPregParams(
      numEntries = 64,    //128
      numRead = None,
      numWrite = None,
    ),
    vfPreg = VfPregParams(
      numEntries = 96,    //160
      numRead = None,
      numWrite = None,
    ),
    v0Preg = V0PregParams(
      numEntries = 8,     //22
      numRead = None,
      numWrite = None,
    ),
    vlPreg = VlPregParams(
      numEntries = 8,     //32
      numRead = None,
      numWrite = None,
    ),
    IntRegCacheSize = 4,
    MemRegCacheSize = 4,
    EnableMiniConfig = true,
    dpParams = DispatchParameters(
      IntDqSize = 8,
      FpDqSize = 8,
      LsDqSize = 8,
      IntDqDeqWidth = 8,
      FpDqDeqWidth = 6,
      VecDqDeqWidth = 6,
      LsDqDeqWidth = 6
    ),
    //Memblock
    VirtualLoadQueueSize = 24,    //56
    LoadQueueRAWSize = 12,        //24
    LoadQueueReplaySize = 24,     //32
    LoadUncacheBufferSize = 4,    //8
    StoreQueueSize = 20,          //32
    StoreBufferSize = 4,          //8
    StoreQueueNWriteBanks = 4,    //8
    StoreBufferThreshold = 3,     //7
    VlMergeBufferSize = 4,        //16
    VsMergeBufferSize = 4,        //16
    VSegmentBufferSize = 4,       //8
    //L1 i&d
    icacheParameters = ICacheParameters(
      nSets = 128, //32 kB ICache, 16*1024/4/64
      nWays = 2,
    ),
    dcacheParametersOpt = Some(DCacheParameters(
      nSets = 128, //32 kB DCache, 16*1024/4/64
      nWays = 2,
      nMissEntries = 4,     //16
      nProbeEntries = 2,    //4
      nReleaseEntries = 2,  //4
      nMaxPrefetchEntry = 2,//6
    )),
    itlbParameters = TLBParameters(
      name = "itlb",
      fetchi = true,
      useDmode = false,
      NWays = 4,
    ),
    dtlbParameters = TLBParameters(
      name = "dtlb",
      NWays = 4,
      outReplace = false,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      saveLevel = false,
      lgMaxSize = 4
    ),
    ldtlbParameters = TLBParameters(
      name = "ldtlb",
      NWays = 4,
      outReplace = false,
      partialStaticPMP = true,
      outsideRecvFlush = true,
      saveLevel = false,
      lgMaxSize = 4
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
