import java.io._
import scala.compat.Platform.EOL
import scala.tools.nsc._

class Transform extends ToolSuite {
  test("simple transform") {
    val testDir = File.createTempFile("testDir", System.nanoTime.toString)
    if (!testDir.delete) sys.error("fatal error: can't delete " + testDir.getAbsolutePath)
    if (!testDir.mkdir) sys.error("fatal error: can't create " + testDir.getAbsolutePath)
    val writer = new PrintWriter(new File(testDir.getAbsolutePath + File.separator + "test.scala"))
    writer.write("""
      |import scala.meta._
      |class main {
      |  inline def apply(defns: Any) = meta {
      |    val q"${obj: meta.Defn.Object}" = defns
      |    println(obj)
      |    ???
      |  }
      |}
    """.stripMargin.trim)
    writer.close

    val (exitCode, output) = runCompiler(testDir, options => Main.main(options ++ Array("-Xprint:typer")))
    if (exitCode != 0) fail("compiling a simple inline def failed: " + EOL + output)

    assert(output.trim === """
      |[[syntax trees at end of                     typer]] // test.scala
      |package <empty> {
      |  import scala.meta._;
      |  class main extends scala.AnyRef {
      |    def <init>(): main = {
      |      main.super.<init>();
      |      ()
      |    };
      |    @scala.meta.internal.inline.inline def apply(defns: Any): Nothing = ???
      |  };
      |  object main$impl extends scala.AnyRef {
      |    def <init>(): main$impl.type = {
      |      main$impl.super.<init>();
      |      ()
      |    };
      |    def apply$impl(prefix: scala.meta.Stat)(defns: scala.meta.Stat): scala.meta.Stat = {
      |      val obj: meta.Defn.Object = (defns: scala.meta.Stat @unchecked) match {
      |        case {
      |  final class $anon extends scala.AnyRef {
      |    def <init>(): <$anon: AnyRef> = {
      |      $anon.super.<init>();
      |      ()
      |    };
      |    def unapply(input: scala.meta.Tree): Option[meta.Defn.Object] = input match {
      |      case (quasiquote$macro$1$hole$0 @ _) => quasiquotes.this.Unlift.unliftIdentity[scala.meta.Tree, meta.Defn.Object]((ClassTag.apply[meta.Defn.Object](classOf[scala.meta.Defn$$Object]): scala.reflect.ClassTag[meta.Defn.Object])).apply(quasiquote$macro$1$hole$0) match {
      |        case (x: meta.Defn.Object)Some[meta.Defn.Object]((quasiquote$macro$1$result$0 @ _)) => scala.Some.apply[meta.Defn.Object](quasiquote$macro$1$result$0)
      |        case _ => scala.None
      |      }
      |    }
      |  };
      |  new $anon()
      |}.unapply(<unapply-selector>) <unapply> ((obj @ (_: meta.Defn.Object))) => obj
      |      };
      |      scala.this.Predef.println(obj);
      |      scala.this.Predef.???
      |    }
      |  }
      |}
    """.trim.stripMargin)
  }
}
