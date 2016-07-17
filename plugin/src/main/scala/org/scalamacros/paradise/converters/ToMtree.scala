package org.scalamacros.paradise
package converters

import org.scalamacros.paradise.reflect._
import scala.reflect.ClassTag
import scala.meta.{Tree => Mtree}

trait ToMtree extends Enrichments
              with ConvertersToolkit { self =>
  protected implicit class XtensionGtreeToMtree(gtree: g.Tree) {
    def toMtree[T <: Mtree : ClassTag]: T = self.toMtree[T](gtree)
  }

  private def toMtree[T <: Mtree : ClassTag](gtree: g.Tree): T = ???
}
