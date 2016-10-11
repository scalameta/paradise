import org.scalameta.paradise.converters.Converter
import org.scalatest._
import scala.{meta => m}
import scala.tools.nsc.{Global, Settings}
import scala.tools.nsc.reporters.ConsoleReporter


class ToMtreeSpec extends FlatSpec with Matchers {

  val g = {
    val settings = new Settings((str: String) => Unit)
    val reporter = new ConsoleReporter(settings)
    new Global(settings, reporter)
  }

  object ToMtreeAdapter extends { val global = g } with Converter {
    def apply(gtree: Any): m.Stat = gtree.asInstanceOf[g.Tree].toMtree[m.Stat]
  }

  object MParser {
    import scala.meta._
    def apply(code: String): m.Tree = code.parse[m.Stat].get
  }

  object GParser {
    import scala.reflect.runtime.universe._
    import scala.tools.reflect.ToolBox
    val tb = runtimeMirror(getClass.getClassLoader).mkToolBox()
    def apply(code: String): g.Tree = tb.parse(code).asInstanceOf[g.Tree]
  }

  def test(className: String, code: String): Unit = {
    it should s"convert $className" in {
      val mtree = MParser(code)
      val gtree = GParser(code)
      val converted = ToMtreeAdapter(gtree)
      println(mtree.structure)
      println(converted.structure)
      converted.structure shouldEqual mtree.structure
    }
  }

  // ============ NAMES ============

  // TODO

  // ============ TERMS ============

  //test("Term.This",                      "this") // XXX fails
  test("Term.Name",                      "foo")
  test("Term.Select",                    "foo.bar")
  test("Term.Apply",                     "foo(bar)")
  test("Term.ApplyType",                 "foo[T]")
  test("Term.Assign",                    "foo = bar")
  //test("Term.Block with single expr",    "{ foo }") // XXX fails
  test("Term.Block with multiple exprs", "{ foo; bar }")
  test("Term.If",                        "if (foo) bar")
  test("Term.If with else",              "if (foo) bar else baz")
  //test("Term.Match",                     "foo match { case bar => baz }") // XXX fails
  test("Term.Function",                  "(foo: Int) => bar")
  test("Term.While",                     "while (foo) bar")
  //test("Term.New",                       "new Foo()") // XXX fails
  test("Term.Arg.Named",                 "foo(bar = baz)")
  //test("Term.Arg.Repeatd",               "foo(bar: _*)") // XXX fails
  test("Term.Arg.Param",                 "def foo(bar: Int = baz) = qux")

  // ============ TYPES ============

  // TODO

  // ============ PATTERNS ============

  // TODO

  // ============ LITERALS ============

  // TODO

  // ============ DECLS ============

  // TODO

  // ============ DEFNS ============

  // TODO

  // ============ PKGS ============

  // TODO

  // ============ CTORS ============

  // TODO

  // ============ TEMPLATES ============

  // TODO

  // ============ MODIFIERS ============

  // TODO

  // ============ ODDS & ENDS ============

  // TODO

}
