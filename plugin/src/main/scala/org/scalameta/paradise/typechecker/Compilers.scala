package org.scalameta.paradise
package typechecker

trait Compilers {
  self: AnalyzerPlugins =>

  import global._
  import analyzer._
  import definitions._
  import paradiseDefinitions._
  import scala.reflect.internal.Flags._

  def mkCompiler(typer: Typer) = new Compiler(typer)
  class Compiler(typer: Typer) {
    val errorGen = new ErrorGen(typer)
    import errorGen._

    private def checkClass(clazz: Symbol): Unit = {
      clazz.addAnnotation(AnnotationInfo(CompileTimeOnlyAttr.tpe, List(Literal(Constant(MacroAnnotationNotExpandedMessage)) setType StringClass.tpe), Nil))
      if (!(clazz isNonBottomSubClass StaticAnnotationClass)) MacroAnnotationMustBeStaticError(clazz)
      // TODO: revisit the decision about @Inherited
      if (clazz.getAnnotation(InheritedAttr).nonEmpty) MacroAnnotationCannotBeInheritedError(clazz)
      if (!clazz.isStatic) MacroAnnotationCannotBeMemberError(clazz)
    }

    def typedOldMacroAnnotation(cdef: ClassDef) = {
      if (!isPastTyper) {
        val clazz = cdef.symbol
        checkClass(clazz)
        clazz.setFlag(MACRO)

        val macroTransform = clazz.info.member(nme.macroTransform)
        def flavorOk = macroTransform.isMacro
        def paramssOk = mmap(macroTransform.paramss)(p => (p.name, p.info)) == List(List((nme.annottees, scalaRepeatedType(AnyTpe))))
        def tparamsOk = macroTransform.typeParams.isEmpty
        def everythingOk = flavorOk && paramssOk && tparamsOk
        if (!everythingOk) OldMacroAnnotationShapeError(clazz)
      }
      cdef
    }

    def typedNewMacroAnnotation(cdef: ClassDef) = {
      if (!isPastTyper) {
        val clazz = cdef.symbol
        checkClass(clazz)
        // NOTE: don't set the MACRO flag to distinguish from old macro annotations

        val apply = clazz.info.member(nme.apply)
        def paramssOk = mmap(apply.paramss)(_.info) == List(List(AnyTpe))
        def retOk = apply.info.finalResultType == AnyTpe
        def tparamsOk = clazz.typeParams.isEmpty
        def everythingOk = paramssOk && retOk && tparamsOk
        if (!everythingOk) NewMacroAnnotationShapeError(clazz)
      }
      cdef
    }
  }
}