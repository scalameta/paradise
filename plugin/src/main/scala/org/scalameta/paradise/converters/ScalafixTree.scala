package org.scalameta.paradise.converters

import scala.{meta => m}
trait ScalafixTree { self: Scalafixer =>

  protected implicit class XtensionGtreeToFixedMtree(gtree: g.Tree) {
    def fix: scala.meta.Tree = self.fix(gtree)
  }

  def fix(gtree: g.Tree): m.Tree = {
    m.Term.Name("???")
  }

}
