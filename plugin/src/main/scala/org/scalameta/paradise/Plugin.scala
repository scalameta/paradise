package org.scalameta.paradise

import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}
import org.scalameta.paradise.typechecker.AnalyzerPlugins
import org.scalameta.paradise.typechecker.HijackAnalyzer
import org.scalameta.paradise.parser.HijackSyntaxAnalyzer

class Plugin(val global: Global) extends NscPlugin with AnalyzerPlugins with HijackSyntaxAnalyzer with HijackAnalyzer {
  import global._

  val name = "macroparadise"
  val description = "Empowers production Scala compiler with latest macro developments"
  val components = Nil
  hijackSyntaxAnalyzer()
  val newAnalyzer = hijackAnalyzer()
  newAnalyzer.addAnalyzerPlugin(AnalyzerPlugin)
  newAnalyzer.addMacroPlugin(MacroPlugin)
}
