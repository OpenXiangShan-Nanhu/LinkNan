package lntest.info

import chisel3.RawModule
import org.chipsalliance.cde.config.Parameters
import xijiang.NodeType
import xs.utils.FileRegisters
import xiangshan.XSCoreParamsKey
import zhujiang.axi.ExtAxiBundle
import zhujiang.{ZJParametersKey, ZhujiangGlobal}

import scala.collection.mutable

object InfoGen {
  private var soc:Option[RawModule] = None
  private val saxiPool = mutable.Queue[ExtAxiBundle]()
  private val maxiPool = mutable.Queue[ExtAxiBundle]()
  def register(in: RawModule):Unit = {
    soc = Some(in)
  }
  def addSaxi(axi: ExtAxiBundle):Unit = {
    saxiPool.addOne(axi)
  }

  def addMaxi(axi: ExtAxiBundle):Unit = {
    maxiPool.addOne(axi)
  }

  def axiStr(m:Boolean):Seq[String] = {
    val prefix = if(m) s"m_axi = {\n" else s"s_axi = {\n"
    val suffix = "},\n"
    val pool = if(m) maxiPool else saxiPool
    val body = pool.map(a => s"  \"${a.pathName}\",\n").toSeq
    prefix +: body :+ suffix
  }

  def generate()(implicit p:Parameters):Unit = {
    val zjP = p(ZJParametersKey)
    val coreP = p(XSCoreParamsKey)
    val nrL2Bank = coreP.L2NBanks
    val socPath = soc.get.pathName
    val zjInfo = ZhujiangGlobal.descStrSeq
    val ccns = zjP.island.filter(_.nodeType == NodeType.CC)
    val ccnStrSeq = ccns.map(n => s"{\"$socPath.cc_${n.domainId}.tile.l2cache\", {0x${(n.nodeId + 1).toHexString}}},\n")
    val l2cStrSeq = s"l2c = {\n" +: ccnStrSeq.map(b => s"  $b") :+ s"  nr_bank = $nrL2Bank,\n" :+ "},\n"
    val result = s"local soc = {\n" +: (zjInfo ++ l2cStrSeq ++ axiStr(true) ++ axiStr(false)).map(b => s"  $b") :+ "}\nreturn soc\n"
    FileRegisters.add("generated-src", "soc.lua", result.reduce(_ ++ _), dontCarePrefix = true)
  }
}
