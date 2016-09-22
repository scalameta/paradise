package org.scalameta.paradise
package reflect

import scala.language.implicitConversions
import scala.tools.nsc.{Global => NscGlobal}
import scala.tools.nsc.{Settings => NscSettings}

trait ReflectToolkit extends Definitions
                        with StdNames
                        with TreeInfo
                        with StdAttachments
                        with Mirrors
                        with Symbols
                        with ReplIntegration
                        with Names
                        with Metadata
                        with LogicalTrees {
  val global: NscGlobal
  lazy val g: global.type = global
  object l extends LogicalTrees
}
