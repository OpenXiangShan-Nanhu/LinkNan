package linknan.fdt

import scala.collection.mutable

class BaseNode(phandle:String, name:String, id:Int) {
  private val nodeName = s"${phandle}_$id:$name"
  private val attrs = mutable.Map[String, String]()
  private val children = mutable.Queue[BaseNode]()
  private val indentDelta = 4

  def addProp(prop:String, value:String):Unit = attrs.addOne((prop, value))

  def addProp(prop:String, value:Int):Unit = attrs.addOne((prop, s"<$value>"))

  def addProp(prop:String):Unit = attrs.addOne((prop, ""))

  def addNode(n:BaseNode):Unit = children.addOne(n)

  def toString(indent:Int):String = {
    val pfx = Seq.fill(indent)(" ").reduce(_ + _)
    val nxtPfx = Seq.fill(indent + indentDelta)(" ").reduce(_ + _)
    val pbody = attrs.map({case(a, b) => if(b.nonEmpty) s"$nxtPfx$a = $b;\n" else s"$nxtPfx$a;\n"}).reduce(_ + _)
    val nbody = children.map(_.toString(indent + indentDelta)).reduce(_ + _)
    s"$pfx$nodeName {\n" + pbody + nbody + s"$pfx};\n"
  }
}
