package org.scalameta.tests

class Semantic extends ConverterSuite {
  semantic("class C { def x = 42 }")
  semantic("class C { val x = 43 }")
  semantic("class C { var x = 43 }")
  semantic("class C { implicit val x = 43 }")
  semantic("class C { private[this] val x = 43 }")
//  semantic("class C { private[C] val x = 43 }") // currently remove C
  semantic("""class C {
             |  def x = 43
             |  val y = 43
             |  var z = 43
             |}""".stripMargin)
}
