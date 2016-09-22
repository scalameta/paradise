package org.scalameta.paradise
package reflect

trait Symbols {
  self: ReflectToolkit =>

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
        val annMethod = sym.info.decl(InlineAnnotationMethodName)
        val annImplMethod = sym.owner.info.decl(sym.name.inlineModuleName).info.decl(InlineAnnotationMethodName.inlineImplName)
        annMethod != NoSymbol && annMethod.initialize.annotations.exists(_.tpe.typeSymbol == MetaInlineClass) && annImplMethod.exists
      }
    }
    def isMacroAnnotation = isOldMacroAnnotation || isNewMacroAnnotation
  }
}
