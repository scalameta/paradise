package org.scalameta.tests

import java.io.{File, PrintWriter}
import scala.collection.immutable.Seq
import scala.{meta => m}
import scala.reflect.io._
import scala.tools.cmd.CommandLineParser
import scala.tools.nsc.{Global, CompilerCommand, Settings}
import scala.tools.nsc.reporters.StoreReporter
import scala.util.control.NonFatal
import org.scalatest._
import org.scalameta.paradise.converters.Converter
import org.scalameta.paradise.mirrors.Mirrors

class ConverterSuite(projectName: String) extends FunSuiteLike {

  // If true, parses code as a compilation unit.
  val parseAsCompilationUnit = false

  lazy val g: Global = {
    def fail(msg: String) = sys.error(s"ReflectToMeta initialization failed: $msg")
    val classpath         = System.getProperty(s"sbt.paths.$projectName.test.classes")
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

  private object converter extends Converter {
    lazy val global: ConverterSuite.this.g.type = ConverterSuite.this.g
    def apply(gtree: g.Tree): m.Tree            = gtree.toMtree[m.Tree]
  }

  lazy val mirror: m.Mirror = Mirrors(g).mirror

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
        case (xs: Seq[_], ys: Seq[_]) =>
          xs.length == ys.length && xs.zip(ys).forall { case (x, y) => loop(x, y) }
        case (x: Tree, y: Tree) =>
          def sameDesugaring = {
            // NOTE: Workaround for https://github.com/scalameta/scalameta/issues/519.
            object TermApply519 {
              def unapply(tree: Tree): Option[(Term, Seq[Seq[Type]], Seq[Seq[Term.Arg]])] =
                tree match {
                  case q"$fun[..$targs](...$argss)" => Some((fun, Seq(targs), argss))
                  case q"$fun(...$argss)"           => Some((fun, Nil, argss))
                  case _                            => None
                }
            }
            object NestedTermAnnotated {
              def flatTerm(t: Term, accum: Seq[Mod.Annot] = Nil): (Term, Seq[Mod.Annot]) =
                t match {
                  case Term.Annotate(t2, as) => flatTerm(t2, as ++ accum)
                  case _                     => (t, accum)
                }
              def unapply(tree: Tree): Option[(Term, Seq[Mod.Annot])] = tree match {
                case t: Term.Annotate => Some(flatTerm(t))
                case _                => None
              }
            }

            try {
              (x, y) match {
                case (TermApply519(q"$xlhs.$xop", xtargss, Seq(xargs)),
                      q"$ylhs $yop [..$ytargs] (..$yargs)") =>
                  loop(xlhs, ylhs) && loop(xop, yop) &&
                    loop(xtargss.flatten, ytargs) && loop(xargs, yargs)
                case (q"{}", q"()") =>
                  true
                case (q"{ $xstat }", q"$ystat") =>
                  loop(xstat, ystat)
                case (ctor"$xctor(...${ Seq() })", ctor"$yctor(...${ Seq(Seq()) })") =>
                  loop(xctor, yctor)
                case (ctor"$xctor(...${ Seq(Seq()) })", ctor"$yctor(...${ Seq() })") =>
                  loop(xctor, yctor)
                case (p"$xpat @ _", p"$ypat") =>
                  loop(xpat, ypat)
                case (p"$xlhs @ (_: $xtpe)", p"$ylhs: $ytpe") =>
                  loop(xlhs, ylhs) && loop(xtpe, ytpe)
                case (t"${ Some(xtpe) } {}", t"$ytpe") =>
                  loop(xtpe, ytpe)
                case (t"$xop[$xlhs, $xrhs]", t"$ylhs $yop $yrhs") =>
                  loop(xlhs, ylhs) && loop(xop, yop) && loop(xrhs, yrhs)
                case (importee"$xfrom => $xto", importee"$yfrom") =>
                  loop(xfrom, yfrom) && xfrom.value == xto.value
                // TODO: Account for `import x, y` being desugared to `import x; import y`.
                // This is not an easy fix, because we need to process both blocks and templates in a non-trivial way.
                // I'm leaving this for future work though, because I think this is gonna be a pretty rare occurrence in tests.
                case (NestedTermAnnotated(xexpr1, xannots1), q"$xexpr2: ..@$xannots2") =>
                  loop(xexpr1, xexpr2) && loop(xannots1, xannots2)
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

  private def createRunFromSnippet(code: String): (g.Run, g.CompilationUnit) = {
    // NOTE: `parseStatsOrPackages` fails to parse abstract type defs without bounds,
    // so we need to apply a workaround to ensure that we correctly process those.
    val rxAbstractTypeNobounds = """^type (\w+)(\[[^=]*?\])?$""".r
    val needsParseWorkaround   = rxAbstractTypeNobounds.unapplySeq(code).isDefined
    val code1                  = if (!needsParseWorkaround) code else code + " <: Dummy"

    val jfile  = File.createTempFile("paradise", ".scala")
    val writer = new PrintWriter(jfile)
    try writer.write(code1)
    finally writer.close()

    val run          = new g.Run
    val abstractFile = AbstractFile.getFile(jfile)
    val sourceFile   = g.getSourceFile(abstractFile)
    val unit         = new g.CompilationUnit(sourceFile)
    run.compileUnits(List(unit), run.phaseNamed("terminal"))

    g.phase = run.parserPhase
    g.globalPhase = run.parserPhase
    val reporter = new StoreReporter()
    g.reporter = reporter
    unit.body = {
      if (parseAsCompilationUnit) {
        g.newUnitParser(unit).parse()
      } else {
        val tree = g.gen.mkTreeOrBlock(g.newUnitParser(unit).parseStatsOrPackages())
        if (!needsParseWorkaround) tree
        else {
          val tdef @ g.TypeDef(mods, name, tparams, _) = tree
          g.treeCopy.TypeDef(tdef, mods, name, tparams, g.TypeBoundsTree(g.EmptyTree, g.EmptyTree))
        }
      }
    }
    val errors = reporter.infos.filter(_.severity == reporter.ERROR)
    errors.foreach(error => fail(s"scalac parse error: ${error.msg} at ${error.pos}"))

    (run, unit)
  }

  private def getParsedScalacTree(code: String): g.Tree = {
    val (run, unit) = createRunFromSnippet(code)
    unit.body
  }

  private def getTypedScalacTree(code: String): g.Tree = {
    import g._
    val (run, unit) = createRunFromSnippet(code)

    val phases   = List(run.parserPhase, run.namerPhase, run.typerPhase)
    val reporter = new StoreReporter()
    g.reporter = reporter

    phases.foreach(phase => {
      g.phase = phase
      g.globalPhase = phase
      phase.asInstanceOf[GlobalPhase].apply(unit)
      val errors = reporter.infos.filter(_.severity == reporter.ERROR)
      errors.foreach(error => fail(s"scalac ${phase.name} error: ${error.msg} at ${error.pos}"))
    })

    unit.body
  }

  private def getParsedMetaTree(code: String): m.Tree = {
    import scala.meta._
    code.parse[m.Stat] match {
      case scala.meta.parsers.Parsed.Success(tree) => tree
      case scala.meta.parsers.Parsed.Error(pos, message, _) =>
        fail(s"meta parse error: $pos at $message")
    }
  }

  def getUnattributedConvertedMetaTree(code: String): m.Tree = {
    converter(getParsedScalacTree(code))
  }

  def getAttributedConvertedMetaTree(code: String): m.Tree = {
    converter(getTypedScalacTree(code))
  }

  private def test(code: String, converter: String => m.Tree): Unit = {
    test(code.trim) {
      val convertedMetaTree = converter(code)
      val parsedMetaTree    = getParsedMetaTree(code)
      try {
        checkMismatchesModuloDesugarings(parsedMetaTree, convertedMetaTree)
      } catch {
        case MismatchException(details) =>
          val header = s"scala -> meta converter error\n$details"
          val fullDetails =
            s"""parsed tree:
               |${parsedMetaTree.structure}
               |converted tree:
               |${convertedMetaTree.syntax}
               |${convertedMetaTree.structure}""".stripMargin
          fail(s"$header\n$fullDetails")
      }
    }
  }

  // TODO: Merge syntactic and semantic into one method in the future.
  // Our current goal is to achieve perfect conversion,
  // i.e. to have the result of syntactic conversion
  // structurally equal to the result of semantic conversion.

  def syntactic(code: String): Unit = {
    test(code, getUnattributedConvertedMetaTree _)
  }

  // TODO: Allow stats as inputs to `semantic`.
  // If we can't parse code as a compilation unit,
  // we should be able to just wrap it in a dummy class and convert that.

  case class Context(code: String, gtree: g.Tree, mtree: m.Tree, mirror: m.Mirror) {
    def tpe(name: String): String = {
      val members = mtree.collect { case m: m.Member if m.name.syntax == name => m }
      members match {
        case Nil                       => sys.error(s"member $name not found")
        case (result: m.Member) :: Nil => mirror.tpe(result).get.syntax
        case _                         => sys.error(s"member $name is ambiguous")
      }
    }
  }

  case class SemanticTest(c: Option[Context]) {
    private var hasRun = false

    def apply(body: Context => Unit): Unit = {
      c match {
        case Some(c) =>
          hasRun = true
          test(c.code)(body(c))
        case _ =>
        // do nothing
      }
    }

    override protected def finalize: Unit = {
      c match {
        case Some(c) =>
          if (hasRun) return
          test(c.code)(())
        case _ =>
        // do nothing
      }
    }
  }

  def semantic(code: String): SemanticTest = {
    val c = try {
      val gtree = getTypedScalacTree(code)
      val mtree = {
        // TODO: also use getAttributedConvertedMetaTree?
        import scala.meta._
        val jfile = g.currentRun.units.next.source.file.file
        if (parseAsCompilationUnit) jfile.parse[m.Source].get
        else jfile.parse[m.Stat].get
      }
      val mirror = this.mirror
      Some(Context(code, gtree, mtree, mirror))
    } catch {
      case NonFatal(ex) =>
        test(code)(throw ex)
        None
    }
    SemanticTest(c)
  }
}
