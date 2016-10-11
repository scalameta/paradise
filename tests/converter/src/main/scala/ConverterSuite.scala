import scala.{meta => m}
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.{Global, CompilerCommand, Settings}
import scala.tools.nsc.reporters.StoreReporter
import org.scalatest._
import org.scalameta.paradise.converters.Converter

trait ConverterSuite extends FunSuite {
  private lazy val g: Global = {
    def fail(msg: String) = sys.error(s"ReflectToMeta initialization failed: $msg")
    val classpath         = System.getProperty("sbt.paths.testsConverter.test.classes")
    val pluginpath        = System.getProperty("sbt.paths.plugin.jar")
    val options           = "-cp " + classpath + " -Xplugin:" + pluginpath + ":" + classpath + " -Xplugin-require:macroparadise"
    val args              = CommandLineParser.tokenize(options)
    val emptySettings     = new Settings(error => fail(s"couldn't apply settings because $error"))
    val reporter          = new StoreReporter()
    val command           = new CompilerCommand(args, emptySettings)
    val settings          = command.settings
    val g                 = new Global(settings, reporter)
    val run               = new g.Run
    g.phase = run.parserPhase
    g.globalPhase = run.parserPhase
    g
  }

  def syntactic(code: String) {
    test(code.trim) {
      val parsedScalacTree: g.Tree = {
        import g._
        val reporter = new StoreReporter()
        g.reporter = reporter
        val tree   = gen.mkTreeOrBlock(newUnitParser(code, "<toolbox>").parseStatsOrPackages())
        val errors = reporter.infos.filter(_.severity == g.reporter.ERROR)
        errors.foreach(error => fail(s"scalac parse error: ${error.msg} at ${error.pos}"))
        tree
      }

      val parsedMetaTree: m.Stat = {
        import scala.meta._
        code.parse[m.Stat] match {
          case scala.meta.parsers.Parsed.Success(tree) => tree
          case scala.meta.parsers.Parsed.Error(pos, message, _) =>
            fail(s"meta parse error: $pos at $message")
        }
      }
      val convertedMetaTree: m.Stat = {
        object converter extends Converter {
          lazy val global: ConverterSuite.this.g.type = ConverterSuite.this.g
          def apply(gtree: g.Tree): m.Stat            = gtree.toMtree[m.Stat]
        }
        converter(parsedScalacTree)
      }

      // TODO: account for the fact that scala.reflect desugars stuff (e.g. for loops) even during parsing
      // TODO: alternatively, we can just go ahead and undesugar for loops, because for syntactic APIs that's actually easy
      if (parsedMetaTree.structure != convertedMetaTree.structure) {
        fail(
          s"scala -> meta converter error\nparsed tree:\n${parsedMetaTree.structure}\nconverted tree\n${convertedMetaTree.structure}")
      }
    }
  }
}
