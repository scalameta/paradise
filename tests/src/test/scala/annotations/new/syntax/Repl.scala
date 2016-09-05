class NewRepl extends ReplSuite {
  // TODO: testing this would require filtering the classpath
  // so that it doesn't have scala.meta libraries anymore
  //
  // test("new macro annotations don't parse without scala.meta") {
  //   assert(repl("""
  //     |object main {
  //     |  inline def apply(x: Int)(defns: Any) = ???
  //     |}
  //   """.stripMargin.trim) === """
  //     |scala> object main {
  //     |  inline def apply(x: Int)(defns: Any) = ???
  //     |<console>:2: error: new-style ("inline") macros require scala.meta
  //     |  inline def apply(x: Int)(defns: Any) = ???
  //     |  ^
  //     |
  //     |scala> }
  //     |<console>:1: error: eof expected but '}' found.
  //     |}
  //     |^
  //   """.stripMargin.trim)
  // }

  // FIXME: issue #4
  // test("new macro annotations compile with scala.meta") {
  //   assert(repl("""
  //     |import scala.meta._
  //     |class main(x: Int) extends scala.annotation.StaticAnnotation {
  //     |  inline def apply(defns: Any): Any = meta(???)
  //     |}
  //   """.stripMargin.trim) === """
  //     |scala> import scala.meta._
  //     |import scala.meta._
  //     |
  //     |scala> class main(x: Int) extends scala.annotation.StaticAnnotation {
  //     |  inline def apply(defns: Any): Any = meta(???)
  //     |}
  //     |defined class main
  //   """.stripMargin.trim)
  // }
}

