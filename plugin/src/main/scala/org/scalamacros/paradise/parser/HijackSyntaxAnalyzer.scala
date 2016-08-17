package org.scalameta.paradise
package parser

import scala.tools.nsc.{Global => NscGlobal, Phase, SubComponent, Settings => NscSettings}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.mutable
import org.scalameta.paradise.parser.{SyntaxAnalyzer => ParadiseSyntaxAnalyzer, ReplGlobal => ParadiseReplGlobal}
import scala.tools.nsc.interpreter.{ReplGlobal => NscReplGlobal, _}
import scala.reflect.internal.util.BatchSourceFile

trait HijackSyntaxAnalyzer {
  self: NscPlugin =>

  // NOTE: Thank you myself from two years ago.
  // https://github.com/scalameta/scalahost/blob/a97d73d7356ede881f9edf335e93db64244ec68d/plugin/src/main/scala/parser/HijackSyntaxAnalyzer.scala
  def hijackSyntaxAnalyzer(): Unit = {
    val newSyntaxAnalyzer = new { val global: self.global.type = self.global } with ParadiseSyntaxAnalyzer
    val syntaxAnalyzerField = classOf[NscGlobal].getDeclaredField("syntaxAnalyzer")
    syntaxAnalyzerField.setAccessible(true)
    syntaxAnalyzerField.set(global, newSyntaxAnalyzer)

    val phasesDescMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesDescMap")
    val phasesDescMap = phasesDescMapGetter.invoke(global).asInstanceOf[mutable.Map[SubComponent, String]]
    val phasesSetMapGetter = classOf[NscGlobal].getDeclaredMethod("phasesSet")
    val phasesSet = phasesSetMapGetter.invoke(global).asInstanceOf[mutable.Set[SubComponent]]
    if (phasesSet.exists(_.phaseName == "parser")) { // `scalac -help` doesn't instantiate standard phases
      phasesSet -= phasesSet.find(_.phaseName == "parser").head
      phasesSet += newSyntaxAnalyzer
      phasesDescMap(newSyntaxAnalyzer) = "parse source into ASTs (with support for inline defs)"
    }

    if (global.isInstanceOf[NscReplGlobal] && global.toString != "<hijacked global>") {
      def obtainField(cls: Class[_], name: String) = { val result = cls.getDeclaredField(name); result.setAccessible(true); result }
      def obtainMethod(cls: Class[_], name: String) = { val result = cls.getDeclaredMethods().filter(_.getName == name).head; result.setAccessible(true); result }
      val f_intp = obtainField(classOf[ReplReporter], "intp")
      val f_compiler = obtainField(classOf[IMain], "_compiler")
      val m_initSources = obtainMethod(classOf[IMain], "_initSources")

      val intp = f_intp.get(global.reporter).asInstanceOf[IMain]
      val settings = new NscSettings()
      intp.settings.copyInto(settings)
      settings.outputDirs setSingleOutput intp.replOutput.dir
      settings.exposeEmptyPackage.value = true
      val hijackedCompiler = new NscGlobal(settings, global.reporter) with ParadiseReplGlobal { override def toString: String = "<hijacked global>" }
      val initSources = m_initSources.invoke(intp).asInstanceOf[List[BatchSourceFile]]
      new hijackedCompiler.Run().compileSources(initSources)
      f_compiler.set(intp, hijackedCompiler)
    }
  }
}