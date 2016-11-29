package org.scalameta.tests

class X extends ConverterSuite {
//  scalafix("class C { implicit val x = 43 }", "")
  scalafix(
    """|package b {
       |  class A[T](a: T)
       |  class B {
       |    class User {
       |      class Bar
       |    }
       |    implicit val user = new User
       |    implicit val bar = new user.Bar
       |    implicit val a = new A(42)
       |    implicit val b = List(1)
       |    def lst: List[_ <: Any] = List(1, "")
       |    implicit val exist = lst
       |    implicit val c = Map(1 -> (3, "string"), 2 -> "1")
       |  }
       |}
       |""".stripMargin,
    """|package b {
       |  class A[T](a: T)
       |  class B {
       |    class User {
       |      class Bar
       |    }
       |    implicit val user: B.this.User = new User
       |    implicit val bar: B.this.user.Bar = new user.Bar
       |    implicit val a: p0.b.A[Int] = new A(42)
       |    implicit val b: List[Int] = List(1)
       |    def lst: List[_ <: Any] = List(1, "")
       |    implicit val exist: List[Any] = lst
       |    implicit val c: scala.collection.immutable.Map[Int,java.io.Serializable] = Map(1 -> (3, "string"), 2 -> "1")
       |  }
       |}""".stripMargin
  )

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
