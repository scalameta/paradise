import org.scalactic.source.Position
import org.scalatest._
import scala.compat.Platform.EOL
import scala.tools.nsc.interpreter._
import scala.tools.nsc.{Settings, MainGenericRunner}

trait ReplSuite extends ToolSuite with DisableScalaColor {
  private def replViaILoop(code: String): String = {
    val s = new Settings
    s.Xnojline.value = true
    s.usejavacp.value = false
    s.classpath.value = sys.props("sbt.paths.tests.classpath")
    s.plugin.value = List(sys.props("sbt.paths.plugin.jar"))
    val lines = ILoop.runForTranscript(code, s).lines.toList
    lines.drop(3).dropRight(2).map(_.replaceAll("\\s+$","")).mkString("\n").trim.stripSuffix("scala>").trim
  }

  private def replViaReplGlobal(code: String): String = {
    val lines = code.trim.stripMargin.split(EOL)
    val input = lines.mkString(EOL) + EOL
    val (exitCode, output) = runRepl(input, options => { MainGenericRunner.main(options ++ Array("-Xnojline")); sys.exit(0) })
    if (exitCode != 0) fail("repl invocation has failed:" + EOL + output)
    val result = {
      // Example output:
      // ===================
      // Welcome to Scala 2.11.8 (Java HotSpot(TM) 64-Bit Server VM, Java 1.7.0_75).
      // Type in expressions for evaluation. Or try :help.
      //
      // scala> import scala.language.experimental.macros
      //
      // scala> import scala.reflect.macros.whitebox.Context
      //
      // scala> import scala.annotation.StaticAnnotation
      //
      // scala>
      // scala>      |      |      |      |      |      |      |      |      |      |      |      |      |      |      |      |      |      |      | defined object thingyAdhocMacro
      //
      // scala>
      // scala>      |      | defined class thingyAdhoc
      //
      // scala>
      // scala> defined class Thingy
      // defined object Thingy
      //
      // scala> defined class Thingy
      // defined object Thingy
      //
      // scala> :quit
      // ===================
      val unwrapped = output.split(EOL).drop(3).dropRight(2).mkString(EOL)
      var i = 0
      """(scala> |     \| )""".r.replaceAllIn(unwrapped, m => {
        var prefix = unwrapped.substring(m.start, m.end)
        if (prefix == "     | ") prefix = ""
        val replacement = prefix + lines(i) + EOL
        i += 1
        replacement
      })
    }
    // NOTE: Trimming here because Sublime trims trailing whitespace in our expected test outputs.
    result.split(EOL).map(line => if (line == "scala> ") "scala>" else line).mkString(EOL)
  }

  // TODO: change this to something less ugly
  private var _repl: String => String = null
  final protected def repl(code: String): String = _repl(code)

  override protected def test(testName: String, testTags: Tag*)(testFun: => Any)(implicit pos: Position): Unit = {
    super.test(testName + " (ILoop)", testTags: _*)({ this._repl = replViaILoop _; testFun })
    super.test(testName + " (ReplGlobal)", testTags: _*)({ this._repl = replViaReplGlobal _; testFun })
  }
}
