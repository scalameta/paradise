package org.scalameta.paradise
package typechecker

trait Errors {
  self: AnalyzerPlugins =>

  import global._
  import analyzer._
  import ErrorUtils._
  import definitions._
  import paradiseDefinitions._

  trait CommonErrorGen {
    def typer: Typer
    lazy implicit val contextTyperErrorGen: Context = typer.infer.getContext

    implicit class XtensionSignature(meth: Symbol) {
      def actualSignature: String = {
        var result = meth.toString
        if (meth.isOverloaded) result += "(...) = ..."
        else if (meth.isMethod) {
          if (meth.typeParams.nonEmpty) {
            def showTparam(tparam: Symbol) =
              tparam.typeSignature match {
                case tpe @ TypeBounds(_, _) => s"${tparam.name}$tpe"
                case _ => tparam.name
              }
            def showTparams(tparams: List[Symbol]) = "[" + (tparams map showTparam mkString ", ") + "]"
            result += showTparams(meth.typeParams)
          }
          if (meth.paramss.nonEmpty) {
            def showParam(param: Symbol) = s"${param.name}: ${param.typeSignature}"
            def showParams(params: List[Symbol]) = {
              val s_mods = if (params.nonEmpty && params(0).hasFlag(scala.reflect.internal.Flags.IMPLICIT)) "implicit " else ""
              val s_params = params map showParam mkString ", "
              "(" + s_mods + s_params + ")"
            }
            def showParamss(paramss: List[List[Symbol]]) = paramss map showParams mkString ""
            result += showParamss(meth.paramss)
          }
          result = result + ": " + meth.info.finalResultType.toString
          if (meth.isTermMacro) result = result.replace("macro method", "def") + " = macro ..."
          else result = result.replace("method", "def") + " = ..."
        }
        if (meth.hasAnnotation(MetaInlineClass)) {
          result = "inline " + result
          result = result.replace("...", "meta { ... }")
        }
        result
      }
    }

    protected def MacroAnnotationShapeError(clazz: Symbol, expected: String, actual: String) = {
      issueSymbolTypeError(clazz, s"""
        |macro annotation has wrong shape:
        |  required: $expected
        |  found   : $actual
      """.trim.stripMargin)
    }

    def MacroAnnotationMustBeStaticError(clazz: Symbol) =
      issueSymbolTypeError(clazz, s"macro annotation must extend scala.annotation.StaticAnnotation")

    def MacroAnnotationCannotBeInheritedError(clazz: Symbol) =
      issueSymbolTypeError(clazz, s"macro annotation cannot be @Inherited")

    def MacroAnnotationCannotBeMemberError(clazz: Symbol) =
      issueSymbolTypeError(clazz, s"macro annotation cannot be a member of another class")

    def MacroAnnotationNotExpandedMessage = {
      "macro annotation could not be expanded " +
      "(the most common reason for that is that you need to enable the macro paradise plugin; " +
      "another possibility is that you try to use macro annotation in the same compilation run that defines it)"
    }

    def MacroAnnotationOnlyDefinitionError(ann: Tree) =
      issueNormalTypeError(ann, "macro annotations can only be put on definitions")

    def MacroAnnotationTopLevelClassWithCompanionBadExpansion(ann: Tree) =
      issueNormalTypeError(ann, "top-level class with companion can only expand into a block consisting in eponymous companions")

    def MacroAnnotationTopLevelClassWithoutCompanionBadExpansion(ann: Tree) =
      issueNormalTypeError(ann, "top-level class without companion can only expand either into an eponymous class or into a block consisting in eponymous companions")

    def MacroAnnotationTopLevelModuleBadExpansion(ann: Tree) =
      issueNormalTypeError(ann, "top-level object can only expand into an eponymous object")

    def MultipleParametersImplicitClassError(tree: Tree) =
      issueNormalTypeError(tree, "implicit classes must accept exactly one primary constructor parameter")
  }

  trait OldErrorGen extends CommonErrorGen {
    def OldMacroAnnotationShapeError(clazz: Symbol) = {
      val meth = clazz.info.member(nme.macroTransform)
      val expected = "def macroTransform(annottees: Any*): Any = macro ..."
      val actual = meth.actualSignature
      MacroAnnotationShapeError(clazz, expected, actual)
    }
  }

  trait NewErrorGen extends CommonErrorGen {
    def NewMacroAnnotationShapeError(clazz: Symbol) = {
      val meth = clazz.info.member(nme.apply)
      val param = meth.paramss match { case List(List(p)) => p.name.toString; case _ => "defn" }
      val expected = s"inline def apply($param: Any): Any = meta { ... }"
      val actual = meth.actualSignature.replace(": Nothing", "")
      MacroAnnotationShapeError(clazz, expected, actual)
    }
  }

  class ErrorGen(val typer: Typer) extends OldErrorGen with NewErrorGen
}
