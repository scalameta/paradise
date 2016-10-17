import scala.collection.immutable.Seq
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

  case class MismatchException(details: String) extends Exception
  private def checkMismatchesModuloDesugarings(parsed: m.Tree, converted: m.Tree): Unit = {
    import scala.meta._
    def loop(x: Any, y: Any): Boolean = {
      val ok = (x, y) match {
        case (x, y) if x == null || y == null =>
          x == null && y == null
        case (x: Some[_], y: Some[_]) =>
          loop(x.get, y.get)
        case (x: None.type, y: None.type) =>
          true
        case (xs: Seq[_], ys: Seq[_])                               =>
          xs.length == ys.length && xs.zip(ys).forall { case (x, y) => loop(x, y) }
        case (x: Tree, y: Tree) =>
          def sameDesugaring = {
            // NOTE: Workaround for https://github.com/scalameta/scalameta/issues/519.
            object TermApply519 {
              def unapply(tree: Tree): Option[(Term, Seq[Seq[Type.Arg]], Seq[Seq[Term.Arg]])] =
                tree match {
                  case q"$fun[..$targs](...$argss)" => Some((fun, Seq(targs), argss))
                  case q"$fun(...$argss)"           => Some((fun, Nil, argss))
                  case _                            => None
                }
            }

            // NOTE: This is a desugaring performed by the scala.reflect parser.
            // We may want to undo it in the converter.
            object TermApplyInfixRightAssoc {
              def unapply(tree: Tree): Option[(Term, Term.Name, Seq[Type.Arg], Seq[Term.Arg])] =
                tree match {
                  case q"{ val $tmp1 = $lhs; ${ TermApply519(q"$rhs.$op", targss, Seq(Seq(tmp2))) } }"
                      if tmp1.syntax == tmp2.syntax && tmp1.syntax.contains("$") =>
                    val args = rhs match {
                      case q"(..$args)" => args
                      case arg          => Seq(arg)
                    }
                    Some((lhs, op, targss.flatten, args))
                  case _ =>
                    None
                }
            }

            try {
              (x, y) match {
                case (q"$xlhs $xop [..$xtargs] (..$xargs)",
                      TermApply519(q"$ylhs.$yop", ytargss, Seq(yargs))) =>
                  loop(xlhs, ylhs) && loop(xop, yop) && loop(xtargs, ytargss.flatten) && loop(
                    xargs,
                    yargs)
                case (q"$xlhs $xop [..$xtargs] (..$xargs)",
                      TermApplyInfixRightAssoc(ylhs, yop, ytargs, yargs)) =>
                  loop(xlhs, ylhs) && loop(xop, yop) && loop(xtargs, ytargs) && loop(xargs, yargs)
                case (q"{}", q"()") =>
                  true
                case (q"{ $xstat }", q"$ystat") =>
                  loop(xstat, ystat)
                case (ctor"$xctor(...${ Seq() })", ctor"$yctor(...${ Seq(Seq()) })") =>
                  loop(xctor, yctor)
                case (xpat, p"$ypat @ _") =>
                  loop(xpat, ypat)
                case (p"$xlhs: $xtpe", p"$ylhs @ (_: $ytpe)") =>
                  loop(xlhs, ylhs)
                case (t"${ Some(xtpe) } {}", t"$ytpe") =>
                  loop(xtpe, ytpe)
                case (t"$xlhs $xop $xrhs", t"$yop[$ylhs, $yrhs]") =>
                  loop(xlhs, ylhs) && loop(xop, yop) && loop(xrhs, yrhs)
                case (importee"$xfrom => $xto", importee"$yfrom") =>
                  loop(xfrom, yfrom) && xfrom.value == xto.value
                case _ =>
                  false
              }
            } catch {
              case _: MismatchException => false
            }
          }
          def sameStructure =
            x.productPrefix == y.productPrefix && loop(x.productIterator.toList,
                                                       y.productIterator.toList)
          sameDesugaring || sameStructure
        case _ =>
          x == y
      }
      if (!ok) throw MismatchException(s"$x != $y")
      else true
    }
    loop(parsed, converted)
  }

  def syntactic(code: String): Unit = {
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

      try {
        checkMismatchesModuloDesugarings(parsedMetaTree, convertedMetaTree)
      } catch {
        case MismatchException(details) =>
          val header = s"scala -> meta converter error\n$details"
          val fullDetails =
            s"parsed tree:\n${parsedMetaTree.structure}\nconverted tree:\n${convertedMetaTree.structure}"
          fail(s"$header\n$fullDetails")
      }
    }
  }
}
