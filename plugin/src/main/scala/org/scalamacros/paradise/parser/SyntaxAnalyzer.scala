package org.scalameta.paradise
package parser

import scala.language.reflectiveCalls
import scala.tools.nsc.ast.parser.{SyntaxAnalyzer => NscSyntaxAnalyzer}
import scala.tools.nsc.ast.parser.BracePatch
import scala.tools.nsc.ast.parser.Tokens._
import scala.tools.nsc.{Global, Phase, SubComponent}
import scala.reflect.internal.Flags // no wildcard import because of ambiguity with Tokens._
import scala.reflect.internal.util.Collections._
import scala.collection.mutable.ListBuffer

abstract class SyntaxAnalyzer extends NscSyntaxAnalyzer {
  import global._
  import definitions._

  val runsAfter = List[String]()
  val runsRightAfter = None
  override val initial = true

  private def initialUnitBody(unit: CompilationUnit): Tree = {
    if (unit.isJava) new JavaUnitParser(unit).parse()
    else if (currentRun.parsing.incompleteHandled) new ParadiseUnitParser(unit).parse()
    else new ParadiseUnitParser(unit).smartParse()
  }

  def newUnitParser(unit: CompilationUnit): UnitParser = new ParadiseUnitParser(unit)
  private class ParadiseUnitParser(unit: global.CompilationUnit, patches: List[BracePatch]) extends UnitParser(unit, Nil) {
    def this(unit: global.CompilationUnit) = this(unit, Nil)
    // override def withPatches(patches: List[BracePatch]): UnitParser = new UnitParser(unit, patches)

    private val MetaInlineClass = rootMirror.getClassIfDefined("scala.meta.internal.inline.inline")
    private val MetaStatClass = rootMirror.getClassIfDefined("scala.meta.Stat")
    private val MetaTypeClass = rootMirror.getClassIfDefined("scala.meta.Type")

    private val INLINEkw = TermName("inline")
    private def isInline = in.token == IDENTIFIER && in.name == INLINEkw && skippingModifiers(in.token == DEF)
    private def skippingModifiers[T](op: => T): T = lookingAhead(if (isModifier) lookingAhead(skippingModifiers(op)) else op)
    override def isExprIntroToken(token: Token) = !isInline && super.isExprIntroToken(token)
    override def isDclIntro: Boolean = isInline || super.isDclIntro

    private def markInline(offset: Offset, mods: Modifiers): Modifiers = {
      if (MetaInlineClass != NoSymbol && MetaStatClass != NoSymbol) mods.withAnnotations(List(atPos(offset)(New(MetaInlineClass))))
      else { syntaxError(offset, "new-style (\"inline\") macros require scala.meta"); mods }
    }

    private def invoke(name: String, args: Any*): Any = {
      val meth = classOf[Parser].getDeclaredMethods().find(_.getName == name).get
      meth.setAccessible(true)
      meth.invoke(this, args.asInstanceOf[Seq[AnyRef]]: _*)
    }
    private def normalizeModifiers(mods: Modifiers): Modifiers = invoke("normalizeModifiers", mods).asInstanceOf[Modifiers]
    private def addMod(mods: Modifiers, mod: Long, pos: Position): Modifiers = invoke("addMod", mods, mod, pos).asInstanceOf[Modifiers]
    private def tokenRange(token: TokenData): Position = invoke("tokenRange", token).asInstanceOf[Position]
    private def flagTokens: Map[Int, Long] = invoke("flagTokens").asInstanceOf[Map[Int, Long]]
    override def modifiers(): Modifiers = normalizeModifiers {
      def loop(mods: Modifiers): Modifiers = in.token match {
        case IDENTIFIER if isInline =>
          val offset = in.offset
          in.nextToken()
          loop(markInline(offset, mods))
        case PRIVATE | PROTECTED =>
          loop(accessQualifierOpt(addMod(mods, flagTokens(in.token), tokenRange(in))))
        case ABSTRACT | FINAL | SEALED | OVERRIDE | IMPLICIT | LAZY =>
          loop(addMod(mods, flagTokens(in.token), tokenRange(in)))
        case NEWLINE =>
          in.nextToken()
          loop(mods)
        case _ =>
          mods
      }
      loop(NoMods)
    }
    override def localModifiers(): Modifiers = {
      def loop(mods: Modifiers): Modifiers = {
        if (isInline) { val offset = in.offset; in.nextToken(); loop(markInline(offset, mods)) }
        else if (isLocalModifier) loop(addMod(mods, flagTokens(in.token), tokenRange(in)))
        else mods
      }
      loop(NoMods)
    }

    // TODO: In the future, it would make sense to perform this transformation during typechecking.
    // However that strategy is going to be much more complicated, so it doesn't fit this prototype.

    override def topStat: PartialFunction[Token, List[Tree]] = {
      case PACKAGE  =>
        packageOrPackageObject(in.skipToken()) :: Nil
      case IMPORT =>
        in.flushDoc
        importClause()
      case _ if isAnnotation || isTemplateIntro || isModifier =>
        joinComment(translateNestedInlineDefs(topLevelTmplDef))
    }

    // NOTE: Here's the sketch of the proposed translation scheme:
    //   * Retain the original signature with the body replaced by ???
    //     (for the analogue of def macros, we can probably turn it into = macro name$impl`,
    //     but for macro annotations it doesn't matter anyway, since it's invoked differently).
    //   * Create a synthetic impl method name$impl that contains a macro context,
    //     replaces type parameters, value parameters as well as the return type with c.Tree.
    //   * Transplant the body to the synthetic method according to the high-friction principle.
    //     Namely: everything is wrapped in an enclosing quasiquote, meta calls are transformed into unquotes.
    //
    // TODO: We need to think how exactly we want to make this work with objects
    // that define inline defs. Do we want to define another object on the side?
    // Do we want to have the synthetic $impl tag along in the original object?
    //
    // class main {
    //   inline def apply(defns: Any) = body
    // }
    // <=======>
    // class main {
    //   @inline def apply(defns: Any) = ???
    // }
    // object main$impl {
    //   def apply$impl(defns: scala.meta.Tree): scala.meta.Tree = {
    //     q"[[ body ]]"
    //   }
    // }
    private def translateNestedInlineDefs(tree: Tree): List[Tree] = {
      tree match {
        case stat @ ClassDef(mods, name, tparams, templ @ Template(parents, self, stats)) =>
          val xstats1 = stats.map {
            case stat @ DefDef(mods, name, tparams, vparamss, tpt, rhs) =>
              def isInline(tpt: Tree) = MetaInlineClass != NoSymbol && tpt.tpe != null && tpt.tpe.typeSymbol == MetaInlineClass
              val inlines = mods.annotations.collect{ case ann @ Apply(Select(New(tpt), nme.CONSTRUCTOR), Nil) if isInline(tpt) => ann }
              if (inlines.nonEmpty) {
                def mkImplVtparam(tdef: TypeDef): ValDef = {
                  atPos(tdef.pos.focus)(ValDef(Modifiers(Flags.PARAM), tdef.name.toTermName, Ident(MetaTypeClass), EmptyTree))
                }
                def mkImplVparam(vdef: ValDef): ValDef = {
                  atPos(vdef.pos.focus)(ValDef(Modifiers(Flags.PARAM), vdef.name, Ident(MetaStatClass), EmptyTree))
                }
                def mkImplTpt(tpt: Tree): Tree = {
                  atPos(tpt.pos.focus)(Ident(MetaStatClass))
                }
                def mkImplBody(body: Tree): Tree = atPos(body.pos.focus)({
                  object transformer extends Transformer {
                    override def transform(tree: Tree): Tree = tree match {
                      // TODO: In the future, it would make sense to perform this transformation during typechecking.
                      // However that strategy is going to be much more complicated, so it doesn't fit this prototype.
                      case Apply(Ident(TermName("meta")), List(arg)) => super.transform(arg)
                      case tree => super.transform(tree)
                    }
                  }
                  transformer.transform(body)
                })
                val signatureMethod = atPos(stat.pos.focus)(DefDef(mods, name, tparams, vparamss, tpt, Ident(Predef_???)))
                val implMethod = atPos(stat.pos.focus)({
                  val implVtparamss = if (tparams.nonEmpty) List(tparams.map(mkImplVtparam)) else Nil
                  val implVparamss = implVtparamss ++ mmap(vparamss)(mkImplVparam)
                  val implTpt = mkImplTpt(tpt)
                  val implBody = mkImplBody(rhs)
                  DefDef(NoMods, TermName(name + "$impl"), Nil, implVparamss, implTpt, implBody)
                })
                (signatureMethod, implMethod)
              } else {
                (stat, EmptyTree)
              }
            case stat =>
              (stat, EmptyTree)
          }
          val (stats1, impls1) = xstats1.unzip
          if (impls1.exists(_.nonEmpty)) {
            val stat1 = atPos(stat.pos)(ClassDef(mods, name, tparams, atPos(templ.pos)(Template(parents, self, stats1))))
            val implmstats = {
              val syntheticCtor = atPos(stat.pos.focus)(DefDef(Modifiers(), termNames.CONSTRUCTOR, List(), List(List()), TypeTree(), Block(List(Apply(Select(Super(This(typeNames.EMPTY), typeNames.EMPTY), termNames.CONSTRUCTOR), List())), Literal(Constant(())))))
              syntheticCtor +: impls1.filter(_.nonEmpty)
            }
            val implmdef = atPos(stat.pos.focus)(ModuleDef(NoMods, TermName(name + "$impl"), Template(List(Ident(TypeName("AnyRef"))), noSelfType, implmstats)))
            List(stat1, implmdef)
          } else {
            List(stat)
          }
        case other =>
          List(other)
      }
    }
  }

  override def newPhase(prev: Phase): StdPhase = new StdPhase(prev) {
    override val checkable = false
    override val keepsTypeParams = false

    def apply(unit: CompilationUnit) {
      informProgress("parsing " + unit)
      // if the body is already filled in, don't overwrite it
      // otherwise compileLate is going to overwrite bodies of synthetic source files
      if (unit.body == EmptyTree)
        unit.body = initialUnitBody(unit)

      if (settings.Yrangepos && !reporter.hasErrors)
        validatePositions(unit.body)

      if (settings.Ymemberpos.isSetByUser)
        new MemberPosReporter(unit) show (style = settings.Ymemberpos.value)
    }
  }
}