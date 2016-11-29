package org.scalameta.paradise.converters

import scala.collection.immutable.Seq
import scala.collection.mutable
import scala.{meta => m}

trait ScalafixTree { self: Scalafixer =>

  protected implicit class XtensionGtreeToFixedMtree(compilationUnit: g.CompilationUnit) {
    def fix: m.Tree = self.fix(compilationUnit)
  }

  def offsetToType(gtree: g.Tree): mutable.Map[Int, m.Type] = {
    val builder = mutable.Map.empty[Int, m.Type]
    def foreach(gtree: g.Tree): Unit = {
      gtree match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty =>
//          import scala.meta._
          import scala.meta.Parsed
          m.dialects.Scala211(tpt.toString()).parse[m.Type] match {
            case Parsed.Success(ast) =>
              builder(tpt.pos.point) = ast
            case _ =>
          }
        case _ =>
      }
      gtree.children.foreach(foreach)
    }
    foreach(gtree)
    builder
  }

  def fix(unit: g.CompilationUnit): m.Tree = {
    val gtree = unit.body
    val o2t   = offsetToType(gtree)
    val mtree = {
      import scala.meta._
      val t = gtree.pos.source.content.parse[m.Source].get
      val patches: Seq[Patch] = t.collect {
        case t @ m.Defn.Val(mods, Seq(pat), None, _) if mods.exists(_.syntax == "implicit") =>
          o2t
            .get(pat.pos.start.offset)
            .map { x =>
              val tok = pat.tokens.last
              Patch(tok, tok, s"$tok: $x")
            }
            .toSeq
      }.flatten
      println("PATHSE: " + patches)
      val m.Source(Seq(pgk)) = Patch.apply(t.tokens, patches).parse[Source].get
      pgk
    }
    println("MTREE: " + mtree)
    mtree
  }

}

import scala.meta._
import scala.meta.tokens.Token
import scala.meta.tokens.Token

/**
  * A patch replaces all tokens between [[from]] and [[to]] with [[replace]].
  */
case class Patch(from: Token, to: Token, replace: String) {
  def insideRange(token: Token): Boolean =
    token.start >= from.start &&
      token.end <= to.end
  val tokens: Seq[Token] = replace.tokenize.get.tokens.to[Seq]
  def runOn(str: Seq[Token]): Seq[Token] = {
    str.flatMap {
      case `from`              => tokens
      case x if insideRange(x) => Nil
      case x                   => Seq(x)
    }
  }
}

object Patch {
  def verifyPatches(patches: Seq[Patch]): Unit = {
    // TODO(olafur) assert there's no conflicts.
  }
  def apply(input: Seq[Token], patches: Seq[Patch]): String = {
    verifyPatches(patches)
    // TODO(olafur) optimize, this is SUPER inefficient
    patches
      .foldLeft(input) {
        case (s, p) => p.runOn(s)
      }
      .map(_.syntax)
      .mkString("")
  }
}
