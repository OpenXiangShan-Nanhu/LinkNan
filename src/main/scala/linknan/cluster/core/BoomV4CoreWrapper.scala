//package linknan.cluster.core
//
//import boom.v4.common._
//import boom.v4.exu.{BoomCore, IssueParams}
//import boom.v4.ifu._
//import boom.v4.lsu.{BoomNonBlockingDCache, LSU}
//import boom.v4.util.BoomCoreStringPrefix
//import chisel3._
//import chisel3.experimental.hierarchy.instantiable
//import freechips.rocketchip.rocket.{DCacheParams, HellaCacheArbiter, ICacheParams, PTW}
//import org.chipsalliance.cde.config.Parameters
//import freechips.rocketchip.tile.{MaxHartIdBits, TileKey, TileVisibilityNodeKey}
//import freechips.rocketchip.tilelink.{TLBuffer, TLEphemeralNode, TLWidthWidget, TLXbar}
//import org.chipsalliance.diplomacy.ValName
//import org.chipsalliance.diplomacy.bundlebridge.BundleBridgeSource
//import org.chipsalliance.diplomacy.lazymodule.LazyModule
//import xs.utils.tl.{TLUserKey, TLUserParams}
//
//import scala.collection.mutable.ListBuffer
//
//class BoomV4CoreWrapper(implicit p:Parameters) extends BaseCoreWrapper {
//  private val visibleNode = TLEphemeralNode()(ValName("tile_master"))
//  private val q = p.alterPartial({
//    case TileKey => BoomTileParams(
//      core = BoomCoreParams(
//        // frontend
//        bpdMaxMetaLength = 45,
//        globalHistoryLength = 16,
//        localHistoryLength = 1,
//        localHistoryNSets = 0,
//        branchPredictor = ((resp_in: BranchPredictionBankResponse, p: Parameters) => {
//          // gshare is just variant of TAGE with 1 table
//          val gshare = Module(new TageBranchPredictorBank(
//            BoomTageParams(tableInfo = Seq((256, 16, 7)))
//          )(p))
//          val btb = Module(new BTBBranchPredictorBank()(p))
//          val bim = Module(new BIMBranchPredictorBank()(p))
//          val preds = Seq(bim, btb, gshare)
//          preds.map(_.io := DontCare)
//
//          bim.io.resp_in(0)  := resp_in
//          btb.io.resp_in(0)  := bim.io.resp
//          gshare.io.resp_in(0) := btb.io.resp
//          (preds, gshare.io.resp)
//        }),
//        //backend
//        fetchWidth = 4,
//        decodeWidth = 2,
//        numRobEntries = 64,
//        issueParams = Seq(
//          IssueParams(issueWidth=2, numEntries=12, iqType=IQ_MEM, dispatchWidth=2),
//          IssueParams(issueWidth=1, numEntries=12, iqType=IQ_UNQ, dispatchWidth=2),
//          IssueParams(issueWidth=2, numEntries=20, iqType=IQ_ALU, dispatchWidth=2),
//          IssueParams(issueWidth=1, numEntries=12, iqType=IQ_FP , dispatchWidth=2)),
//        numIntPhysRegisters = 64,
//        numFpPhysRegisters = 64,
//        numIrfReadPorts = 5,
//        numIrfBanks = 2,
//        numFrfReadPorts = 3,
//        numFrfBanks = 1,
//        numLdqEntries = 16,
//        numStqEntries = 16,
//        maxBrCount = 12,
//        numFetchBufferEntries = 16,
//        ftq = FtqParameters(nEntries=32),
//        nPerfCounters = 6,
//        fpu = Some(freechips.rocketchip.tile.FPUParams(sfmaLatency=4, dfmaLatency=4, divSqrt=true))
//      ),
//      dcache = Some(
//        DCacheParams(rowBits = p(DcacheKey).rowBits, nSets = p(DcacheKey).nSets, nWays = p(DcacheKey).nWays, nMSHRs=8, nTLBWays=8)
//      ),
//      icache = Some(
//        ICacheParams(rowBits = 64, nSets=64, nWays=4, fetchBytes=2*4)
//      ),
//      tileId = 0
//    )
//    case TLUserKey => TLUserParams(aliasBits = p(DcacheKey).aliasBitsOpt.getOrElse(0))
//    case MaxHartIdBits => 16
//    case TileVisibilityNodeKey => visibleNode
//  })
//  private val tileParams = q(TileKey)
//  private val tileId = tileParams.tileId
//  private val tlMasterXbar = LazyModule(new TLXbar(nameSuffix = Some(s"MasterXbar")))
//  private lazy val dcache = LazyModule(new BoomNonBlockingDCache(tileId)(q))
//  private lazy val frontend = LazyModule(new BoomFrontend(tileParams.icache.get, tileId)(q))
//  private val resetVectorNode = BundleBridgeSource(frontend.resetVectorSinkNode.genOpt)
//
//  tlMasterXbar.node := TLBuffer() := TLWidthWidget(tileParams.dcache.get.rowBits/8) := visibleNode := dcache.node
//  tlMasterXbar.node := TLBuffer() := TLWidthWidget(tileParams.icache.get.fetchBytes) := frontend.masterNode
//  cioNode :*= TLBuffer.chainNode(1, Some(s"uncache_buffer")) :*= tlMasterXbar.node
//  l2Node :*= TLBuffer.chainNode(1, Some(s"l2_buffer")) :*= TLWidthWidget(8) :*= tlMasterXbar.node
//  frontend.resetVectorSinkNode := resetVectorNode
//
//  lazy val module = new BoomV4CoreWrapperImpl
//  @instantiable
//  class BoomV4CoreWrapperImpl extends BaseCoreWrapperImpl(this) {
//    resetVectorNode.out.head._1 := io.reset_vector
//
//    private val core = Module(new BoomCore()(q))
//    private val lsu = Module(new LSU()(q, dcache.module.edge))
//    private val ptwPorts = ListBuffer(lsu.io.ptw, frontend.module.io.ptw, core.io.ptw_tlb)
//
//    core.io.interrupts.debug := io.dbip
//    core.io.interrupts.mtip := io.mtip
//    core.io.interrupts.msip := io.msip
//    core.io.interrupts.meip := io.meip
//    core.io.interrupts.seip.get := io.seip
//    core.io.hartid := io.mhartid
//    cpuHalt := false.B
//
//    frontend.module.io.cpu <> core.io.ifu
//    core.io.lsu <> lsu.io.core
//    core.io.rocc := DontCare
//
//    private val ptw = Module(new PTW(ptwPorts.length)(dcache.node.edges.out.head, q))
//    core.io.ptw <> ptw.io.dpath
//    ptw.io.requestor <> ptwPorts.toSeq
//
//    private val hellaCacheArb = Module(new HellaCacheArbiter(1)(q))
//    hellaCacheArb.io.requestor.head <> ptw.io.mem
//    lsu.io.hellacache <> hellaCacheArb.io.mem
//    dcache.module.io.lsu <> lsu.io.dmem
//
//    private val frontendStr = frontend.module.toString
//    private val coreStr = core.toString
//    private val boomTileStr =
//      (BoomCoreStringPrefix(s"======BOOM Tile $tileId Params======")(q) + "\n"
//        + frontendStr
//        + coreStr + "\n")
//    override def toString: String = boomTileStr
//    print(boomTileStr)
//  }
//}
