package org.scalameta.paradise
package reflect

import scala.reflect.internal.Flags._
import scala.reflect.internal.MissingRequirementError

trait Definitions {
  self: ReflectToolkit =>

  import global._
  import rootMirror._
  import definitions._

  object paradiseDefinitions {
    lazy val InheritedAttr = requiredClass[java.lang.annotation.Inherited]
    lazy val MetaInlineClass = rootMirror.getClassIfDefined("scala.meta.internal.inline.inline")
    lazy val MetaStatClass = rootMirror.getClassIfDefined("scala.meta.Stat")
    lazy val MetaTypeClass = rootMirror.getClassIfDefined("scala.meta.Type")
  }
}
