package org.scalamacros.paradise
package converters

import scala.tools.nsc.Global
import scala.reflect.internal.{Flags, HasFlags}
import scala.reflect.internal.Flags._
import scala.collection.mutable
import org.scalameta.invariants._
import org.scalameta.unreachable
import org.scalameta.adt._
import org.scalameta.roles._

trait LogicalTrees { self: ConvertersToolkit =>

  import global.{require => _, abort => _, _}
  import definitions._
  import treeInfo._
  import build._

  trait LogicalTrees { l: self.l.type =>
    // ============ NAMES ============

    trait Name extends Tree

    case class AnonymousName() extends Name with TermParamName with TypeParamName with QualifierName

    trait QualifierName extends Name

    trait TermParamName extends Name

    trait TypeParamName extends Name

    case class IndeterminateName(value: String) extends Name with QualifierName

    implicit class RichNameTree(tree: Tree) {
      def displayName: String = tree match {
//        case tree: ModuleDef if tree.name == nme.PACKAGE => abort(tree)
//        case tree: NameTree => tree.name.displayName
//        case This(name) => name.displayName // NOTE: This(tpnme.EMPTY) is also accounted for
//        case Super(_, name) => name.displayName
        case tree: l.IndeterminateName => tree.value
//        case tree: l.TermName => tree.value
//        case tree: l.TypeName => tree.value
//        case tree: l.CtorName => tree.value
//        case _ => unreachable(debug(tree, showRaw(tree)))
      }
    }

    // ============ TERMS ============

    object TermThis {
      // qual
      def unapply(tree: g.This): Option[l.QualifierName] = {
        if (tree.qual == tpnme.EMPTY) Some(l.AnonymousName())
        else Some(l.IndeterminateName(tree.displayName))
      }
    }



  }

  // ============ ROLES (UNIVERSAL) ============

  sealed private trait Location {
    def test(tree: g.Tree): Boolean = {
      val actual = {
        if (tree.isInstanceOf[g.PackageDef] || tree.metadata.contains("isLogicalToplevel")) ToplevelLoc
        else if (tree.metadata.contains("isLogicalType")) TypeLoc
        else if (tree.isInstanceOf[g.CaseDef] || tree.metadata.contains("isLogicalPattern")) PatLoc
        else if (tree.metadata.contains("isLogicalParam")) ParamLoc
        else if (tree.metadata.contains("isLogicalSelf")) SelfLoc
        else if (tree.metadata.contains("isLogicalSupercall")) SupercallLoc
        else if (tree.isInstanceOf[g.MemberDef] || tree.isInstanceOf[g.Import]) StatLoc
        else if (tree.isInstanceOf[g.Template]) TemplateLoc
        else TermLoc
      }
      this == actual
    }
    def mark[U <: g.Tree](tree: U): U = this match {
      case ToplevelLoc => tree.appendMetadata("isLogicalToplevel" -> true)
      case StatLoc => tree.removeMetadata("isLogicalParam", "isLogicalSelf")
      case ParamLoc => tree.appendMetadata("isLogicalParam" -> true)
      case TermLoc => tree.removeMetadata("isLogicalType", "isLogicalPattern", "isLogicalSupercall")
      case TypeLoc => tree.appendMetadata("isLogicalType" -> true)
      case PatLoc => tree.appendMetadata("isLogicalPattern" -> true)
      case TemplateLoc => require(tree.isInstanceOf[Template]); tree
      case SupercallLoc => tree.appendMetadata("isLogicalSupercall" -> true)
      case SelfLoc => tree.appendMetadata("isLogicalSelf" -> true)
    }
  }

  @role private object ToplevelLoc extends Location with Role[g.Tree]
  @role private object StatLoc extends Location with Role[g.Tree]
  @role private object ParamLoc extends Location with Role[g.Tree]
  @role private object TermLoc extends Location with Role[g.Tree]
  @role private object TypeLoc extends Location with Role[g.Tree]
  @role private object PatLoc extends Location with Role[g.Tree]
  @role private object TemplateLoc extends Location with Role[g.Tree]
  @role private object SupercallLoc extends Location with Role[g.Tree]
  @role private object SelfLoc extends Location with Role[g.Tree]

  // ============ ROLES (TYPEDEF) ============

  @role private object TypeMemberRole extends Role[g.TypeDef] {
    def test(tree: g.TypeDef) = tree.is(StatLoc)
    def mark[U <: g.TypeDef](tree: U) = tree.set(StatLoc)
  }

  @role private class TypeParamRole(lvbounds: List[g.Tree], lcbounds: List[g.Tree]) extends Role[g.TypeDef]
  object TypeParamRole {
    def get(tree: g.TypeDef): Option[TypeParamRole] = {
      if (!tree.is(ParamLoc)) return None
      val attachment = tree.metadata.get("logicalTparam").map(_.asInstanceOf[TypeParamRole])
      attachment.orElse(Some(TypeParamRole(Nil, Nil)))
    }
    def set[U <: g.TypeDef](tree: U, c: TypeParamRole): U = {
      val tree1 = tree.set(ParamLoc)
      if (c.lvbounds.isEmpty && c.lcbounds.isEmpty) tree1
      else tree1.appendMetadata("logicalTparam" -> tree1)
    }
  }

  @role private class SelfRole(lowner: l.Name) extends Role[g.ValDef]
  object SelfRole {
    def get(tree: g.ValDef): Option[SelfRole] = {
      if (!tree.is(SelfLoc)) return None
      val lowner = tree.metadata("logicalOwner").asInstanceOf[l.Name]
      Some(SelfRole(lowner))
    }
    def set[U <: g.ValDef](tree: U, c: SelfRole): U = {
      tree.set(SelfLoc).appendMetadata("logicalOwner" -> c.lowner)
    }
  }

  // ============ ROLES (DEFDEF) ============

  @role private object AbstractMethodRole extends ReadonlyRole[g.DefDef] {
    def test(tree: g.DefDef) = tree.rhs.isEmpty
  }

  @role private object MethodRole extends ReadonlyRole[g.DefDef] {
    def test(tree: g.DefDef) = tree.name != nme.CONSTRUCTOR && tree.rhs.nonEmpty && !tree.is(MacroRole)
  }

  @role private object MacroRole extends ReadonlyRole[g.DefDef] {
    def test(tree: g.DefDef) = tree.mods.hasFlag(MACRO) || tree.symbol.hasFlag(MACRO)
  }

  @role private class PrimaryCtorRole(lowner: l.Name) extends Role[g.DefDef]
  object PrimaryCtorRole {
    def get(tree: g.DefDef): Option[PrimaryCtorRole] = {
      if (tree.name != nme.CONSTRUCTOR) return None
      if (!tree.hasMetadata("isLogicalPrimaryCtor")) return None
      val lowner = tree.metadata("logicalOwner").asInstanceOf[l.Name]
      Some(PrimaryCtorRole(lowner))
    }
    def set[U <: g.DefDef](tree: U, c: PrimaryCtorRole): U = {
      tree.appendMetadata("isLogicalPrimaryCtor" -> true, "logicalOwner" -> c.lowner)
    }
  }

  @role private class SecondaryCtorRole(lowner: l.Name) extends Role[g.DefDef]
  object SecondaryCtorRole {
    def get(tree: g.DefDef): Option[SecondaryCtorRole] = {
      if (tree.name != nme.CONSTRUCTOR) return None
      if (tree.hasMetadata("isLogicalPrimaryCtor")) return None
      val lowner = tree.metadata("logicalOwner").asInstanceOf[l.Name]
      Some(SecondaryCtorRole(lowner))
    }
    def set[U <: g.DefDef](tree: U, c: SecondaryCtorRole): U = {
      tree.removeMetadata("isLogicalPrimaryCtor").appendMetadata("logicalOwner" -> c.lowner)
    }
  }

  // ============ ROLES (CLASSDEF) ============

  @role private object ClassRole extends ReadonlyRole[g.ClassDef] {
    def test(tree: g.ClassDef) = !tree.is(TraitRole)
  }

  @role private object TraitRole extends ReadonlyRole[g.ClassDef] {
    def test(tree: g.ClassDef) = tree.mods.hasFlag(TRAIT) || tree.symbol.hasFlag(TRAIT)
  }

  // ============ ROLES (MODULEDEF) ============

  @role private object ObjectRole extends ReadonlyRole[g.ModuleDef] {
    def test(tree: g.ModuleDef) = !tree.is(PackageObjectRole)
  }

  @role private object PackageObjectRole extends ReadonlyRole[g.ModuleDef] {
    def test(tree: g.ModuleDef) = tree.name == nme.PACKAGE
  }

  // ============ ROLES (PACKAGEDEF) ============

  @role private object PackageRole extends ReadonlyRole[g.PackageDef] {
    def test(tree: g.PackageDef) = !tree.is(EmptyPackageRole) && !tree.is(PackageObjectPackageRole)
  }

  @role private object EmptyPackageRole extends ReadonlyRole[g.PackageDef] {
    def test(tree: g.PackageDef) = tree.name == nme.EMPTY_PACKAGE_NAME
  }

  @role private class PackageObjectPackageRole(gmdef: g.ModuleDef) extends ReadonlyRole[g.PackageDef]
  object PackageObjectPackageRole {
    def get(tree: g.PackageDef) = tree match {
      case g.PackageDef(pid, List(mdef: g.ModuleDef)) if mdef.name == nme.PACKAGE =>
        require(pid.name != nme.EMPTY_PACKAGE_NAME)
        Some(PackageObjectPackageRole(mdef))
      case _ =>
        None
    }
  }

  // ============ ROLES (TEMPLATE) ============

  @role private class TemplateRole(lowner: l.Name) extends Role[g.Template]
  object TemplateRole {
    def get(tree: g.Template): Option[TemplateRole] = {
      val lowner = tree.metadata("logicalOwner").asInstanceOf[l.Name]
      Some(TemplateRole(lowner))
    }
    def set[U <: g.Template](tree: U, c: TemplateRole): U = {
      tree.appendMetadata("logicalOwner" -> c.lowner)
    }
  }

  // ============ ROLES (MISC) ============

//  @role private class SupercallRole(lsuperctor: l.Symbol) extends Role[g.Tree]
//  object SupercallRole {
//    def get(tree: g.Tree): Option[SupercallRole] = {
//      if (!tree.is(SupercallLoc)) return None
//      val lsuperctor = tree.metadata("logicalSuperctor").asInstanceOf[l.Symbol]
//      Some(SupercallRole(lsuperctor))
//    }
//    def set[U <: g.Tree](tree: U, c: SupercallRole): U = {
//      tree.set(SupercallLoc).appendMetadata("logicalSuperctor" -> c.lsuperctor)
//    }
//  }
}
