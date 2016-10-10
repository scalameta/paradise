package scala.meta.paradise.converters

import scala.reflect.internal.util.ScalaClassLoader
import scala.tools.nsc.Driver
import scala.{meta => m}
import scala.tools.nsc.Global
import scala.tools.nsc.Settings
import scala.tools.nsc.typechecker.Analyzer
import scala.tools.reflect.ReflectGlobal
import scala.tools.util.PathResolver

import org.scalameta.paradise.converters.Converter
import org.scalatest.FunSuite

// I have no idea if this is correct, just blatantly copy pasted from
// https://github.com/xeno-by/backupler-2013-07-25/blob/36d82e926920e997e9556870754ac5e9c1f85650/src/compiler/scala/tools/reflect/ReflectMain.scala
object ReflectMain extends Driver {

  private def classloaderFromSettings(settings: Settings) = {
    val classpath = new PathResolver(settings).result
    ScalaClassLoader.fromURLs(classpath.asURLs, getClass.getClassLoader)
  }

  override def newCompiler(): Global = {
    val settings = new Settings
    new ReflectGlobal(settings, reporter, classloaderFromSettings(settings))
  }
}

class ToMtreeTest extends FunSuite {

  val g = ReflectMain.newCompiler()

  object ToMtreeAdapter extends { val global = g } with Converter with Analyzer {
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

  def check(original: String, desugared: String): Unit = {
    test(original) {
      val mtree     = MParser(desugared)
      val gtree     = GParser(original)
      val converted = ToMtreeAdapter(gtree)
      println(mtree.structure)
      assert(converted.structure == mtree.structure)
    }
  }
  def check(original: String): Unit = {
    check(original, original)
  }

  check("println(1)")

}
