package lntest.info

import chisel3.RawModule
import org.chipsalliance.cde.config.Parameters
import xijiang.NodeType
import xs.utils.FileRegisters
import zhujiang.{ZJParametersKey, ZhujiangGlobal}

object InfoGen {
  private var soc:Option[RawModule] = None
  def register(in: RawModule):Unit = {
    soc = Some(in)
  }

  def generate()(implicit p:Parameters):Unit = {
    val zjP = p(ZJParametersKey)
    val socPath = soc.get.pathName
    val zjInfo = ZhujiangGlobal.getRingDesc(zjP.ciName)
    val ccns = zjP.island.filter(_.nodeType == NodeType.CC)
    val ccnStrSeq = ccns.map(n => s"{\"$socPath.cc_${n.domainId}.tile.l2cache\", {0x${(n.nodeId + 1).toHexString}}},\n")
    val l2cStrSeq = s"l2c = {\n" +: ccnStrSeq.map(b => s"  $b") :+ "},\n"
    val result = s"soc = {\n" +: (zjInfo ++ l2cStrSeq).map(b => s"  $b") :+ "}\n"
    FileRegisters.add("generated-src", "soc.lua", result.reduce(_ ++ _), dontCarePrefix = true)
  }
}
