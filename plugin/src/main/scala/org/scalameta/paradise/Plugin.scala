package org.scalameta.paradise

import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}
import scala.collection.{immutable, mutable}

import org.scalameta.paradise.reflect.ReflectToolkit
import org.scalameta.paradise.converters.Converter
import org.scalameta.paradise.converters.PersistPhase
import org.scalameta.paradise.parser.HijackSyntaxAnalyzer
import org.scalameta.paradise.typechecker.HijackAnalyzer
import org.scalameta.paradise.typechecker.AnalyzerPlugins
import org.scalameta.paradise.backend.HijackBackend
import org.scalameta.paradise.converters.ScalafixPhase

class Plugin(val global: Global)
    extends NscPlugin
    with PersistPhase
    with ScalafixPhase
    with HijackSyntaxAnalyzer
    with HijackAnalyzer
    with AnalyzerPlugins
    with HijackBackend {
  val name        = "macroparadise"
  val description = "Empowers production Scala compiler with latest macro developments"
  val components = {
    val persistEnabled  = sys.props("persist.enable") != null
    val scalafixEnabled = sys.props("scalafix.enable") != null
    if (persistEnabled) List[NscPluginComponent](PersistComponent)
    else if (scalafixEnabled || true) List[NscPluginComponent](ScalafixComponent)
    else Nil
  }

  hijackSyntaxAnalyzer()
  val newAnalyzer = hijackAnalyzer()
  newAnalyzer.addAnalyzerPlugin(AnalyzerPlugin)
  newAnalyzer.addMacroPlugin(MacroPlugin)
  val (newBackend, oldBackend) = hijackBackend()
  // TODO: looks like it doesn't get hijacked cleanly...
  // if (global.genBCode ne newBackend) sys.error("failed to hijack backend")
}
