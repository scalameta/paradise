package org.scalameta.paradise
package mirrors

import scala.collection.mutable
import scala.tools.nsc.Global
import scala.util.control.NonFatal
import scala.{meta => m}
import scala.meta.dialects.Scala211
import org.scalameta.paradise.reflect.ReflectToolkit
import org.scalameta.paradise.converters.Converter

// NOTE: Initial implementation of Mirror is taken from
// https://github.com/scalacenter/scalafix/blob/v0.2.0/scalafix-nsc/src/main/scala/scalafix/nsc/NscSemanticApi.scala

trait Mirrors extends ReflectToolkit with Converter {
  private implicit class XtensionPosition(gpos: scala.reflect.internal.util.Position) {
    def matches(mpos: m.Position): Boolean = {
      gpos.isDefined &&
      gpos.start == mpos.start.offset &&
      gpos.end == mpos.end.offset
    }
  }

  /** Returns a map from byte offset to type name at that offset. */
  private def offsetToType(gtree: g.Tree): mutable.Map[Int, m.Type] = {
    case class SemanticContext(enclosingPackage: String, inScope: List[String])

    // TODO(olafur) Come up with more principled approach, this is hacky as hell.
    // Operating on strings is definitely the wrong way to approach this
    // problem. Ideally, this implementation uses the tree/symbol api. However,
    // while I'm just trying to get something simple working I feel a bit more
    // productive with this hack.
    val builder = mutable.Map.empty[Int, m.Type]
    def add(gtree: g.Tree, ctx: SemanticContext): Unit = {

      /** Removes redudant Foo.this.ActualType prefix from a type */
      val stripRedundantThis: m.Type => m.Type = _.transform {
        case m.Term.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
        case m.Type.Select(m.Term.This(m.Name.Indeterminate(_)), qual) =>
          qual
      }.asInstanceOf[m.Type]

      val stripImportedPrefix: m.Type => m.Type = _.transform {
        case prefix @ m.Type.Select(_, name) if ctx.inScope.contains(prefix.syntax) =>
          name
      }.asInstanceOf[m.Type]

      val stripEnclosingPackage: m.Type => m.Type = _.transform {
        case typ: m.Type.Ref =>
          import scala.meta._
          typ.syntax.stripPrefix(ctx.enclosingPackage).parse[m.Type].get
      }.asInstanceOf[m.Type]

      val cleanUp: (m.Type) => m.Type =
        stripRedundantThis andThen
          stripImportedPrefix andThen
          stripEnclosingPackage

      val parsed = Scala211(gtree.toString()).parse[m.Type]
      parsed match {
        case m.parsers.Parsed.Success(ast) =>
          builder(gtree.pos.point) = cleanUp(ast)
        case _ =>
      }
    }

    def members(tpe: g.Type): Iterable[String] = tpe.members.collect {
      case x if !x.fullName.contains("$") =>
        x.fullName
    }

    def evaluate(ctx: SemanticContext, gtree: g.Tree): SemanticContext = {
      gtree match {
        case g.ValDef(_, _, tpt, _) if tpt.nonEmpty => add(tpt, ctx)
        case g.DefDef(_, _, _, _, tpt, _)           => add(tpt, ctx)
        case _                                      =>
      }
      gtree match {
        case g.PackageDef(pid, _) =>
          val newCtx = ctx.copy(enclosingPackage = pid.symbol.fullName + ".",
                                inScope = ctx.inScope ++ members(pid.tpe))
          gtree.children.foldLeft(newCtx)(evaluate)
          ctx // leaving pkg scope
        case t: g.Template =>
          val newCtx =
            ctx.copy(inScope = t.symbol.owner.fullName :: ctx.inScope)
          gtree.children.foldLeft(newCtx)(evaluate)
          ctx
        case g.Import(expr, selectors) =>
          val newNames: Seq[String] = selectors.collect {
            case g.ImportSelector(from, _, to, _) if from == to =>
              Seq(s"${expr.symbol.fullName}.$from")
            case g.ImportSelector(_, _, null, _) =>
              members(expr.tpe)
          }.flatten
          ctx.copy(inScope = ctx.inScope ++ newNames)
        case _ =>
          gtree.children.foldLeft(ctx)(evaluate)
      }
    }
    evaluate(SemanticContext("", Nil), gtree)
    builder
  }

  private def collect[T](gtree: g.Tree)(pf: PartialFunction[g.Tree, T]): Seq[T] = {
    val builder = Seq.newBuilder[T]
    val f       = pf.lift
    def iter(gtree: g.Tree): Unit = {
      f(gtree).foreach(builder += _)
      gtree match {
        case t @ g.TypeTree() if t.original != null && t.original.nonEmpty =>
          iter(t.original)
        case _ =>
          gtree.children.foreach(iter)
      }
    }
    iter(gtree)
    builder.result()
  }

  implicit object mirror extends m.Mirror {
    private def enclosingGtree(tree: m.Tree): g.Tree = {
      val m.inputs.Input.File(file, _) = tree.pos.input
      val unit                         = global.currentRun.units.find(_.source.file.file == file).get
      unit.body
    }

    def tpe(member: m.Member): m.Completed[m.Type] = apiBoundary {
      val offsets = offsetToType(enclosingGtree(member))
      member match {
        case m.Defn.Val(_, Seq(pat), _, _) =>
          offsets(pat.pos.start.offset)
        case m.Defn.Def(_, name, _, _, _, _) =>
          offsets(name.pos.start.offset)
        case _ =>
          throw new m.SemanticException(member.pos, s"unsupported member $member")
      }
    }

    def desugar(tree: m.Tree): m.Completed[m.Tree] = apiBoundary {
      assert(g.settings.Yrangepos.value)
      val result = collect(enclosingGtree(tree)) {
        case t if t.pos.matches(tree.pos) =>
          val snippet = m.Input.String(t.toString())
          val result = tree match {
            case term: m.Term => Scala211(snippet).parse[m.Term]
            case tpe: m.Type  => Scala211(snippet).parse[m.Type]
            case pat: m.Pat   => Scala211(snippet).parse[m.Pat]
          }
          result.get
      }
      result.head
    }

    private def apiBoundary[T](body: => T): m.Completed[T] = {
      try {
        m.Completed.Success(body)
      } catch {
        case NonFatal(ex: Exception) =>
          m.Completed.Error(ex)
      }
    }
  }
}

object Mirrors {
  def apply[G <: Global](global0: G): Mirrors { val global: G } = {
    new Mirrors { val global: G = global0 }
  }
}
