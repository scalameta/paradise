package org.scalameta.paradise
package reflect

trait Symbols {
  self: Enrichments =>

  import global._
  import scala.reflect.internal.Flags._

  implicit class ParadiseSymbol(sym: Symbol) {
    def isAnnotationMacro = {
      // NOTE: no equivalent of this in new-style ("inline") macros
      sym.isTermMacro && sym.owner.isMacroAnnotation && sym.name == nme.macroTransform
    }
    def isOldMacroAnnotation = {
      sym.isClass && sym.hasFlag(MACRO)
    }
    def isNewMacroAnnotation = {
      sym.isClass && {
        val MetaInlineClass = rootMirror.getClassIfDefined("scala.meta.internal.inline.inline")
        val applyMethod = sym.info.decl(TermName("apply"))
        val applyImplMethod = sym.owner.info.decl(TermName(sym.name + "$impl")).info.decl(TermName("apply$impl"))
        applyMethod != NoSymbol && applyMethod.initialize.annotations.exists(_.tpe.typeSymbol == MetaInlineClass) && applyImplMethod.exists
      }
    }
    def isMacroAnnotation = isOldMacroAnnotation || isNewMacroAnnotation
  }
}
