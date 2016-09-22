package org.scalameta.paradise

import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{mutable, immutable}
import org.scalameta.paradise.reflect.ReflectToolkit
import org.scalameta.paradise.converters.Converter
import org.scalameta.paradise.parser.HijackSyntaxAnalyzer
import org.scalameta.paradise.typechecker.HijackAnalyzer
import org.scalameta.paradise.typechecker.AnalyzerPlugins

class Plugin(val global: Global) extends NscPlugin
                                    with HijackSyntaxAnalyzer
                                    with HijackAnalyzer
                                    with AnalyzerPlugins {
  val name = "macroparadise"
  val description = "Empowers production Scala compiler with latest macro developments"
  val components = Nil

  hijackSyntaxAnalyzer()
  val newAnalyzer = hijackAnalyzer()
  newAnalyzer.addAnalyzerPlugin(AnalyzerPlugin)
  newAnalyzer.addMacroPlugin(MacroPlugin)
}
