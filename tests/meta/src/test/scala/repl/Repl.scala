class Repl extends ReplSuite("testsMeta") {
  test("new macro annotations expand with scala.meta") {
    val printout = repl("""
      |import scala.meta._
      |class main(x: Int) extends scala.annotation.StaticAnnotation {
      |  inline def apply(defn: Any): Any = meta { defn }
      |}
      |@main(42) class C
    """.stripMargin.trim)
    assert(printout.contains("defined class main"))
    assert(printout.contains("defined object main$inline"))
    assert(printout.contains("defined class C"))
  }
}
