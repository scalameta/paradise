class Repl extends ReplSuite("testsReflect") {
  test("precompiled macros expand") {
    val printout = repl("""
      |@thingy class Thingy
      |@thingy class NonThingy
    """.stripMargin.trim)
    assert(printout.contains("defined class Thingy"))
    assert(printout.contains("defined object Thingy"))
    assert(!printout.contains("defined class NonThingy"))
    assert(!printout.contains("defined object NonThingy"))
  }

  test("ad-hoc macros expand") {
    val printout = repl("""
      |import scala.language.experimental.macros
      |import scala.reflect.macros.whitebox.Context
      |import scala.annotation.StaticAnnotation
      |
      |object thingyAdhocMacro {
      |  def impl(c: Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {
      |    import c.universe._
      |    val toEmit = c.Expr(q"class Thingy(i: Int) { def stuff = println(i) }; object Thingy { def apply(x: Int) = new Thingy(x) }")
      |    annottees.map(_.tree) match {
      |      case Nil => {
      |        c.abort(c.enclosingPosition, "No test target")
      |      }
      |      case (classDeclaration: ClassDef) :: Nil => {
      |        // println("No companion provided")
      |        toEmit
      |      }
      |      case (classDeclaration: ClassDef) :: (companionDeclaration: ModuleDef) :: Nil => {
      |        // println("Companion provided")
      |        toEmit
      |      }
      |      case _ => c.abort(c.enclosingPosition, "Invalid test target")
      |    }
      |  }
      |}
      |
      |class thingyAdhoc extends StaticAnnotation {
      |  def macroTransform(annottees: Any*): Any = macro thingyAdhocMacro.impl
      |}
      |
      |@thingyAdhoc class Thingy
      |@thingyAdhoc class NonThingy
    """.stripMargin.trim)
    assert(printout.contains("defined class Thingy"))
    assert(printout.contains("defined object Thingy"))
    assert(!printout.contains("defined class NonThingy"))
    assert(!printout.contains("defined object NonThingy"))
  }
}
