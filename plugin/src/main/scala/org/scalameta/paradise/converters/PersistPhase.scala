package org.scalameta.paradise
package converters

import scala.{meta => m}
import scala.meta._
import scala.reflect.internal.util.SourceFile
import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.tools.nsc.plugins.{Plugin => NscPlugin, PluginComponent => NscPluginComponent}

import java.nio.file.Files
import java.nio.file.Paths

import org.scalameta.paradise.reflect.ReflectToolkit

trait PersistPhase extends ReflectToolkit with Converter {
  object PersistComponent extends NscPluginComponent {
    lazy val global: PersistPhase.this.global.type = PersistPhase.this.global
    import global._

    // TODO: ideally we would like to save everything after the very end of typechecking, which is after refchecks
    // but unfortunately by then a lot of semantic stuff is already desugared to death (patmat, superaccessors, some code in refchecks)
    // therefore we run after typer and hope for the best (i.e. that we don't run into nonsense that we don't know how to convert,
    // and also that we don't encounter residual cyclic reference errors which are the reason why certain typechecks are delayed past typer)
    // btw this isn't such a big problem for persistence, but it definitely is for macro interpretation
    // let's hope that the research into runtime macros, which entails moving the typechecker to scala-reflect.jar will allow us to restructure things
    // so that delayed typechecks come right after typer, not intermingled with other logic
    override val runsAfter      = List("typer")
    override val runsRightAfter = None
    override val phaseName      = "persist"
    override def description    = "persist scala.meta trees"

    private var _backendCheck = false
    private def ensureBCodeBackend(): Unit = {
      if (!_backendCheck) {
        _backendCheck = true
        if (!settings.isBCodeActive) {
          global.reporter
            .error(NoPosition, "scala.meta tree persistence requires -Ybackend:GenBCode")
        }
      }
    }

    override def newPhase(prev: Phase): Phase = new Phase(prev) {
      override def name = "persist"
      override def run(): Unit = {
        global.currentRun.units.foreach(unit => {
          if (sys.props("persist.debug") != null)
            println(s"computing scala.meta tree for ${unit.source.file.path}")
          unit.body.metadata("scalameta") = unit.body.toMtree[Source]
          ensureBCodeBackend() // NOTE: actual persistence is delayed until bytecode emission
        })
      }
    }
  }
}

trait ScalafixPhase extends ReflectToolkit with Scalafixer {
  object ScalafixComponent extends NscPluginComponent {
    lazy val global: ScalafixPhase.this.global.type = ScalafixPhase.this.global
    import global._

    override val runsAfter      = List("typer")
    override val runsRightAfter = None
    override val phaseName      = "scalafix"
    override def description    = "run scalafix rewrites"

    override def newPhase(prev: Phase): Phase = new Phase(prev) {
      override def name = "scalafix"
      override def run(): Unit = {
        println("HELLO FROM COMPILER!")
        global.currentRun.units.foreach(unit => {
          val fixedTree = unit.fix
          Files.write(Paths.get(unit.source.path), fixedTree.getBytes("UTF-8"))
        })
      }
    }
  }
}
