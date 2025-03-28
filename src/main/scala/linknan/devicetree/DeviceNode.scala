package linknan.devicetree

object IntegerType extends Enumeration {
  type IntegerType = Value
  val U32  = Value(0, "U32")
  val U64   = Value("U64")
}

sealed trait DeviceTreeValue {
  def toString:String
}

case class StringValue(value: String) extends DeviceTreeValue {
  override def toString: String = "\"" + value + "\""
}

case class IntegerValue(value: Long, utype:IntegerType.IntegerType = IntegerType.U32) extends DeviceTreeValue {
  override def toString: String = if(utype == IntegerType.U32) {
    s"<$value>"
  } else {
    s"<${(value >> 32).toInt} ${value & 0xFFFF_FFFF}>"
  }
}

case class HexValue(value: Long, utype:IntegerType.IntegerType = IntegerType.U64) extends DeviceTreeValue {
  override def toString: String = if(utype == IntegerType.U32) {
    s"<0x${value.toHexString}>"
  } else {
    s"<0x${(value >> 32).toInt.toHexString} 0x${(value & 0xFFFF_FFFF).toInt.toHexString}>"
  }
}

case class RegValue(base:Long, size:Long, addrCell:Int = 2, sizeCell:Int = 1) extends DeviceTreeValue {
  require(addrCell > 0)
  private def cellStr(data:Long, cells:Int) = {
    Seq.tabulate(cells)(i => "0x" + (0xFFFF_FFFF & (data >> (cells - i - 1) * 32)).toHexString).reduce((a:String, b:String) => s"$a $b")
  }
  override def toString:String = {
    val addrStrs = cellStr(base, addrCell)
    val sizeStrs = cellStr(size, sizeCell)
    s"<$addrStrs $sizeStrs>"
  }
}

case class IntrValue(intrSeq: Seq[(String, Int)]) extends DeviceTreeValue {
  override def toString:String = {
    val tmp = intrSeq.map({case(a, b) => s"&$a $b"}).reduce((a:String, b:String) => s"$a $b")
    s"<$tmp>"
  }
}

case class FlagValue() extends DeviceTreeValue {
  override def toString: String = ""
}

case class ReferenceValue(ref: String) extends DeviceTreeValue {
  override def toString: String = s"&$ref"
}

case class PropertyValues(values: List[DeviceTreeValue]) extends DeviceTreeValue {
  override def toString: String = values.mkString("", ", ", "")
}

case class Property(name: String, value: DeviceTreeValue) {
  override def toString:String = value match {
    case FlagValue() => s"$name;"
    case _ => s"$name = ${value.toString};"
  }
}

class DeviceNode(
  val name: String,
  val label: String = "",
  val properties: List[Property] = Nil,
  val children: List[DeviceNode] = Nil
) {
  def withLabel(_label: String): DeviceNode = copy(label = _label)
  def withProperty(prop: Property): DeviceNode = copy(properties = properties :+ prop)
  def withChild(child: DeviceNode): DeviceNode = copy(children = children :+ child)
  def withProperties(props:List[Property]):DeviceNode = copy(properties = properties ++ props)

  def copy(
    name: String = name,
    label: String = label,
    properties: List[Property] = properties,
    children: List[DeviceNode] = children
  ): DeviceNode = {
    new DeviceNode(name, label, properties, children)
  }
}