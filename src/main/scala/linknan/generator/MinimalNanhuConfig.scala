package linknan.generator

import org.chipsalliance.cde.config.Config
import xiangshan.XSCoreParamsKey
import xiangshan.backend.regfile.{IntPregParams, V0PregParams, VfPregParams, VlPregParams}
import xiangshan.cache.DCacheParameters
import xiangshan.frontend.icache.ICacheParameters

class MinimalNanhuConfig extends Config((site, here, up) => {
  case XSCoreParamsKey => up(XSCoreParamsKey).copy(
    //Frontend
    IBufSize = 32, //32
    FtqSize = 32, //8
    //Backend
    RobSize = 72, //96
    RabSize = 96, //96
    intPreg = IntPregParams(
      numEntries = 64,  //128
      numRead = None,
      numWrite = None,
    ),
    vfPreg = VfPregParams(
      numEntries = 120,  //160
      numRead = None,
      numWrite = None,
    ),
    v0Preg = V0PregParams(
      numEntries = 8, //22
      numRead = None,
      numWrite = None,
    ),
    vlPreg = VlPregParams(
      numEntries = 8, //32
      numRead = None,
      numWrite = None,
    ),
    //Memblock
    VirtualLoadQueueSize = 24,    //56
    LoadQueueRAWSize = 12,        //24
    LoadQueueReplaySize = 12,     //32
    LoadUncacheBufferSize = 4,    //8
    StoreQueueSize = 16,          //32
    StoreBufferSize = 4,          //8
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
    ))
  )
})
