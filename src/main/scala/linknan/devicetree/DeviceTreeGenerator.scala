package linknan.devicetree

import org.chipsalliance.cde.config.Parameters
import xijiang.NodeType
import xs.utils.FileRegisters
import zhujiang.ZJParametersKey

object DeviceTreeGenerator {
  private def indent(level: Int): String = "  " * level

  private def formatLabels(label: String): String = if (label != "") label + ": " else ""

  private def formatProperty(prop: Property): String = prop.toString

  private def formatNode(node: DeviceNode, indentLevel: Int = 0): String = {
    val sb = new StringBuilder

    sb.append(s"${indent(indentLevel)}${formatLabels(node.label)}${node.name} {\n")

    node.properties.foreach { prop =>
      sb.append(s"${indent(indentLevel + 1)}${formatProperty(prop)}\n")
    }

    node.children.foreach { child =>
      sb.append(s"${formatNode(child, indentLevel + 1)}\n")
    }

    sb.append(s"${indent(indentLevel)}};")
    sb.toString
  }

  def lnGenerate(implicit p:Parameters): Unit = {
    val cpuNum = p(ZJParametersKey).island.filter(_.nodeType == NodeType.CC).map(_.cpuNum).sum
    val root = new DeviceNode(name = "/")
      .withProperties(List(
        Property("#address-cells", IntegerValue(2)),
        Property("#size-cells", IntegerValue(2))
      ))
      .withChild(CpuNode(cpuNum))
      .withChild(SocNode(cpuNum))

    FileRegisters.add("software", s"dtsi", s"/dts-v1/;\n\n${formatNode(root)}")
  }

  def simGenerate(implicit p:Parameters):Unit = {
    val dts = new DeviceNode(name = "/")
      .withChild(new DeviceNode(name = "soc", children = List(SerialNode())))
      .withChild(ChosenNode())
      .withChild(MemNode())

    FileRegisters.add("software", s"LNSim.dts", s"/dts-v1/;\n/include/ \"LNTop.dtsi\"\n${formatNode(dts)}", dontCarePrefix = true)
  }
}