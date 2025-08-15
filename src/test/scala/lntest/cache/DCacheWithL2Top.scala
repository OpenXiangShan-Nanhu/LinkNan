package lntest.cache

import circt.stage.FirtoolOption
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselGeneratorAnnotation
import org.chipsalliance.cde.config.{Config, Parameters}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.tile.MaxHartIdBits
import xiangshan.cache._
import xiangshan.{XSCoreParamsKey, XSCoreParameters}
import xs.utils.stage.XsStage
import coupledL2.prefetch.PrefetchReceiverParams
import coupledL2.tl2chi.{DecoupledCHI, DecoupledPortIO, PortIO, TL2CHICoupledL2, TL2CHIL2Module, CHIIssue, Issue}
import coupledL2.tl2chi.{CHIREQ, CHIRSP, CHIDAT, CHISNP}
import xs.utils.perf.{LogUtilsOptionsKey, LogUtilsOptions, DebugOptionsKey, DebugOptions, PerfCounterOptionsKey, PerfCounterOptions, XSPerfLevel}
import xs.utils.debug.{HardwareAssertionKey, HwaParams}
import xs.utils.cache.{EnableCHI, L1Param, L2Param}
import xs.utils.cache.prefetch.BOPParameters
import xs.utils.cache.common.{L2ParamKey, BankBitsKey}

class SplitCHIREQ()(implicit p: Parameters) extends TL2CHIL2Module {
  val io = IO(new Bundle {
    val mergedFlit = Input(UInt((new CHIREQ).getWidth.W))
    val splitFlit = Output(new CHIREQ)
  })

  var lsb = 0
  io.splitFlit.getElements.reverse.foreach {
    case e =>
      val elementWidth = e.asUInt.getWidth
      e := io.mergedFlit.asUInt(lsb + elementWidth - 1, lsb).asTypeOf(e.cloneType)
      lsb += elementWidth
  }
}

class SplitCHIRSP(reverse: Boolean = false)(implicit p: Parameters) extends TL2CHIL2Module {
  val io = IO(new Bundle {
    val mergedFlit = if(reverse) Output(UInt((new CHIRSP).getWidth.W)) else Input(UInt((new CHIRSP).getWidth.W))
    val splitFlit = if(reverse) Input(new CHIRSP) else Output(new CHIRSP)
  })

  if(reverse) {
    val mergedBits = io.splitFlit.getElements.map(_.asUInt)
    io.mergedFlit := Cat(mergedBits)
  } else {
    var lsb = 0
    io.splitFlit.getElements.reverse.foreach {
      case e =>
        val elementWidth = e.asUInt.getWidth
        e := io.mergedFlit.asUInt(lsb + elementWidth - 1, lsb).asTypeOf(e.cloneType)
        lsb += elementWidth
    }
  }
}

class SplitCHIDAT(reverse: Boolean = false)(implicit p: Parameters) extends TL2CHIL2Module {
  val io = IO(new Bundle {
    val mergedFlit = if(reverse) Output(UInt((new CHIDAT).getWidth.W)) else Input(UInt((new CHIDAT).getWidth.W))
    val splitFlit = if(reverse) Input(new CHIDAT) else Output(new CHIDAT)
  })

  if(reverse) {
    val mergedBits = io.splitFlit.getElements.map(_.asUInt)
    io.mergedFlit := Cat(mergedBits)
  } else {
    var lsb = 0
    io.splitFlit.getElements.reverse.foreach {
      case e =>
        val elementWidth = e.asUInt.getWidth
        e := io.mergedFlit.asUInt(lsb + elementWidth - 1, lsb).asTypeOf(e.cloneType)
        lsb += elementWidth
    }
  }
}

class SplitCHISNP(reverse: Boolean = false)(implicit p: Parameters) extends TL2CHIL2Module {
  val io = IO(new Bundle {
    val mergedFlit = if(reverse) Output(UInt((new CHISNP).getWidth.W)) else Input(UInt((new CHISNP).getWidth.W))
    val splitFlit = if(reverse) Input(new CHISNP) else Output(new CHISNP)
  })

  if(reverse) {
    val mergedBits = io.splitFlit.getElements.map(_.asUInt)
    io.mergedFlit := Cat(mergedBits)
  } else {
    var lsb = 0
    io.splitFlit.getElements.reverse.foreach {
      case e =>
        val elementWidth = e.asUInt.getWidth
        e := io.mergedFlit.asUInt(lsb + elementWidth - 1, lsb).asTypeOf(e.cloneType)
        lsb += elementWidth
    }
  }
}

class SimpleEndpointCHI()(implicit p: Parameters) extends TL2CHIL2Module {
    val io = IO(new Bundle {
        val chi = Flipped(new PortIO(splitFlit = true))
    })

    val fakeCHIBundle = WireInit(0.U.asTypeOf(new PortIO(splitFlit = true)))
    io.chi <> fakeCHIBundle

    // Keep clock and reset
    val (_, cnt) = Counter(true.B, 10)
    dontTouch(cnt)

    dontTouch(io)
}

class CHIEmptyShell(splitFlit: Boolean = false)(implicit p: Parameters) extends  TL2CHIL2Module {
  val io = IO(new Bundle {
    val chiIn = Flipped(new PortIO(splitFlit = splitFlit))
    val chiOut = new PortIO(splitFlit = splitFlit)
  })

  io.chiIn <> io.chiOut
  dontTouch(io)
  dontTouch(clock)
  dontTouch(reset)
}

class DCacheWithL2Top()(implicit p: Parameters) extends LazyModule {
  def createTLULNode(name: String, source: Int) = {
    val masterNode = TLClientNode(
      Seq(
        TLMasterPortParameters.v1(
          clients = Seq(
            TLMasterParameters.v1(
              name = name,
              sourceId = IdRange(0, source)
            )
          )
        )
      )
    )
    masterNode
  }

  val nrBank = 2
  val BlockSize = 64
  val bankBinder = BankBinder(nrBank, BlockSize)

  val dcache = LazyModule(new DCache())
  val l2cache = LazyModule(new TL2CHICoupledL2())
  val mmioNode = createTLULNode("mmio", 16)

  val prefetchSourceNode = l2cache.pf_recv_node.map(n => BundleBridgeSource(n.genOpt.get))
  l2cache.pf_recv_node.foreach(_ := prefetchSourceNode.get)

  l2cache.managerNode :=* TLXbar() :=* bankBinder :*= l2cache.node :*= TLBuffer() :*= TLXbar() :=* dcache.clientNode
  l2cache.mmioNode := mmioNode

  lazy val module = new LazyModuleImp(this) {
    override def resetType = Module.ResetType.Asynchronous

    val io = IO(new Bundle {
      val dcache = new DCacheIO()
      // val chi = new PortIO()
    })
    val prefetch = prefetchSourceNode.map(_.makeIOs())

    io.dcache <> dcache.module.io
    // io.chi <> l2cache.module.io_chi

    l2cache.module.io := DontCare
    l2cache.module.io_nodeID := 0.U
    l2cache.module.io.l2_hint <> dcache.module.io.l2_hint

    val chiEndpoint = Some(Module(new SimpleEndpointCHI()))

    val chiEmptyShell = Module(new CHIEmptyShell())
    chiEmptyShell.io.chiIn <> l2cache.module.io_chi

    chiEndpoint.get.io.chi.rxsactive <> chiEmptyShell.io.chiOut.rxsactive
    chiEndpoint.get.io.chi.txsactive <> chiEmptyShell.io.chiOut.txsactive
    chiEndpoint.get.io.chi.syscoack <> chiEmptyShell.io.chiOut.syscoack
    chiEndpoint.get.io.chi.syscoreq <> chiEmptyShell.io.chiOut.syscoreq
    chiEndpoint.get.io.chi.tx.linkactiveack <> chiEmptyShell.io.chiOut.tx.linkactiveack
    chiEndpoint.get.io.chi.tx.linkactivereq <> chiEmptyShell.io.chiOut.tx.linkactivereq
    chiEndpoint.get.io.chi.rx.linkactiveack <> chiEmptyShell.io.chiOut.rx.linkactiveack
    chiEndpoint.get.io.chi.rx.linkactivereq <> chiEmptyShell.io.chiOut.rx.linkactivereq

    val in_rx = chiEndpoint.get.io.chi.rx
    val out_rx = chiEmptyShell.io.chiOut.rx

    in_rx.rsp.flitpend <> out_rx.rsp.flitpend
    in_rx.rsp.flitv <> out_rx.rsp.flitv
    in_rx.rsp.lcrdv <> out_rx.rsp.lcrdv
    val splitRXRSP = Module(new SplitCHIRSP(reverse = true))
    out_rx.rsp.flit := splitRXRSP.io.mergedFlit
    splitRXRSP.io.splitFlit := in_rx.rsp.flit


    in_rx.dat.flitpend <> out_rx.dat.flitpend
    in_rx.dat.flitv <> out_rx.dat.flitv
    in_rx.dat.lcrdv <> out_rx.dat.lcrdv
    val splitRXDAT = Module(new SplitCHIDAT(reverse = true))
    out_rx.dat.flit := splitRXDAT.io.mergedFlit
    splitRXDAT.io.splitFlit := in_rx.dat.flit

    
    in_rx.snp.flitpend <> out_rx.snp.flitpend
    in_rx.snp.flitv <> out_rx.snp.flitv
    in_rx.snp.lcrdv <> out_rx.snp.lcrdv
    val splitRXSNP = Module(new SplitCHISNP(reverse = true))
    out_rx.snp.flit := splitRXSNP.io.mergedFlit
    splitRXSNP.io.splitFlit := in_rx.snp.flit


    val in_tx = chiEndpoint.get.io.chi.tx
    val out_tx = chiEmptyShell.io.chiOut.tx
    in_tx.req.flitpend <> out_tx.req.flitpend
    in_tx.req.flitv <> out_tx.req.flitv
    in_tx.req.lcrdv <> out_tx.req.lcrdv
    val splitTXREQ = Module(new SplitCHIREQ)
    splitTXREQ.io.mergedFlit := out_tx.req.flit
    in_tx.req.flit := splitTXREQ.io.splitFlit


    in_tx.rsp.flitpend <> out_tx.rsp.flitpend
    in_tx.rsp.flitv <> out_tx.rsp.flitv
    in_tx.rsp.lcrdv <> out_tx.rsp.lcrdv
    val splitTXRSP = Module(new SplitCHIRSP)
    splitTXRSP.io.mergedFlit := out_tx.rsp.flit
    in_tx.rsp.flit := splitTXRSP.io.splitFlit


    in_tx.dat.flitpend <> out_tx.dat.flitpend
    in_tx.dat.flitv <> out_tx.dat.flitv
    in_tx.dat.lcrdv <> out_tx.dat.lcrdv
    val splitTXDAT = Module(new SplitCHIDAT)
    splitTXDAT.io.mergedFlit := out_tx.dat.flit
    in_tx.dat.flit := splitTXDAT.io.splitFlit
  }
}

// mill -i linknan.test.runMain lntest.cache.DCacheWithL2Top -td build/DCacheWithL2Top
object DCacheWithL2Top extends App {
  val xsCoreParams = XSCoreParameters()
  val dcacheParams = xsCoreParams.dcacheParametersOpt.get
  val config = new Config((_, _, _) => {
    case L2ParamKey => L2Param(
      clientCaches = Seq(L1Param(
        sets = 128,
        ways = 4,
        vaddrBitsOpt = Some(48),
        aliasBitsOpt = dcacheParams.aliasBitsOpt
      )),
      prefetch = Seq(BOPParameters(), PrefetchReceiverParams()),
    )
    case CHIIssue => Issue.Eb
    case EnableCHI => true
    case DecoupledCHI => false // TODO
    case XSCoreParamsKey => xsCoreParams
    case BankBitsKey => 2
    case MaxHartIdBits => 1
    case LogUtilsOptionsKey => LogUtilsOptions(false, false, true)
    case DebugOptionsKey => DebugOptions(EnablePerfDebug = false, EnableHWMoniter = false)
    case PerfCounterOptionsKey => PerfCounterOptions(
      enablePerfPrint = false,
      enablePerfDB = false,
      perfLevel = XSPerfLevel.NORMAL,
      perfDBHartID = 0
    )
    case HardwareAssertionKey => HwaParams(enable = false)
  })
  val top = DisableMonitors(p => LazyModule(new DCacheWithL2Top()(p)))(config)

  (new XsStage).execute(args ++ Seq("--target", "systemverilog", "--split-verilog"), Seq(
    FirtoolOption("-O=release"),
    FirtoolOption("--disable-all-randomization"),
    FirtoolOption("--disable-annotation-unknown"),
    FirtoolOption("--strip-debug-info"),
    FirtoolOption("--lower-memories"),
    FirtoolOption(
      "--lowering-options=noAlwaysComb," +
        " disallowPortDeclSharing, disallowLocalVariables," +
        " emittedLineLength=120, explicitBitcast, locationInfoStyle=plain," +
        " disallowExpressionInliningInPorts, disallowMuxInlining"
    )
  ) :+ ChiselGeneratorAnnotation(() => top.module) )
}
