package org.scalamacros.paradise
package converters

import org.scalamacros.paradise.reflect.{Metadata, Enrichments}

import scala.reflect.internal.Trees

trait ConvertersToolkit extends Enrichments
                        with LogicalTrees
                        with Metadata {
  lazy val g: global.type = global
  object l extends LogicalTrees {

  }
}
