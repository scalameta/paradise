package org.scalameta.paradise
package reflect

import scala.collection.{immutable, mutable}

trait Trees { self: ReflectToolkit =>

  import global._
  import scala.reflect.internal.Flags._

  implicit class XtensionPat(tree: Tree) {
    def childrenPatterns: Set[Tree] = {
      val patterns = mutable.Set[g.Tree]()
      object traverser extends g.Traverser {
        override def traverse(tree: Tree): Unit = {
          tree match {
            case g.CaseDef(pat, _, _) =>
              object traverser extends g.Traverser {
                override def traverse(tree: Tree): Unit = {
                  tree match {
                    case g.Ident(nme.WILDCARD) =>
                      patterns += tree
                    case g.Ident(_) =>
                      // converted as term
                    case g.Select(_, _) =>
                      // converted as term
                    case g.Bind(_, body) =>
                      patterns += tree
                      traverse(body)
                    case g.Alternative(alts) =>
                      patterns += tree
                      alts.foreach(traverse)
                    case g.Typed(ident, tpat) =>
                      patterns += ident
                      patterns += tree
                    case g.Apply(_, args) =>
                      patterns += tree
                      args.foreach(traverse)
                    case _ =>
                      // do nothing special
                  }
                }
              }
              traverser.traverse(pat)
            case _ =>
              // do nothing special
          }
          super.traverse(tree)
        }
      }
      traverser.traverse(tree)
      patterns.toSet
    }
  }
}
