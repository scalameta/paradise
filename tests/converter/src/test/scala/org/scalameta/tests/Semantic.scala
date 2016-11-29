package org.scalameta.tests

class X extends ConverterSuite {
  scalafix("class C { implicit val x = 43 }")
}

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
  semantic("""class A(a: Int)""")
  semantic("class B { implicit val goodbye = List(1) }")
  semantic("""package b {
             |  class A(a: Int)
             |  class B {
             |    implicit val a = new A(42)
             |  }
             |}
           """.stripMargin)
//  semantic("class B { val goodbye = scala.collection.immutable.List(1) }") // modulo desugaring is tricky for this case.
}
