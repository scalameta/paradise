package org.scalameta.paradise
package reflect

trait Names { self: ReflectToolkit =>

  import global._

  implicit class XtensionInlineManglingName(name: Name) {
    def inlineModuleName: TermName = TermName(name.toString.inlineModuleName)
    def inlineImplName: TermName   = TermName(name.toString.inlineImplName)
  }

  implicit class XtensionInlineManglingString(s: String) {
    def inlineModuleName: String = s.toString + "$inline"
    def inlineImplName: String   = s.toString
  }

  lazy val InlinePrefixParameterName  = TermName("prefix")
  lazy val InlineAnnotationMethodName = TermName("apply")

  implicit class RichFoundationHelperName(name: g.Name) {
    def isAnonymous = {
      val isTermPlaceholder        = name.isTermName && name.startsWith(nme.FRESH_TERM_NAME_PREFIX)
      val isTypePlaceholder        = name.isTypeName && name.startsWith("_$")
      val isAnonymousSelf          = name.isTermName && (name.startsWith(nme.FRESH_TERM_NAME_PREFIX) || name == nme.this_)
      val isAnonymousTypeParameter = name == tpnme.WILDCARD
      isTermPlaceholder || isTypePlaceholder || isAnonymousSelf || isAnonymousTypeParameter
    }
    def looksLikeInfix = {
      val hasSymbolicName =
        !name.decoded.forall(c => Character.isLetter(c) || Character.isDigit(c) || c == '_')
      val idiomaticallyUsedAsInfix    = name == nme.eq || name == nme.ne
      val idiomaticallyNotUsedAsInfix = name == nme.CONSTRUCTOR
      (hasSymbolicName || idiomaticallyUsedAsInfix) && !idiomaticallyNotUsedAsInfix
    }
    def isRightAssoc = {
      name.decoded.endsWith(":")
    }
    def displayName = {
      // NOTE: "<empty>", the internal name for empty package, isn't a valid Scala identifier, so we hack around
      if (name == null || name == rootMirror.EmptyPackage.name || name == rootMirror.EmptyPackageClass.name)
        "_empty_"
      // TODO: why did we need this in the past?
      // else if (name.isAnonymous) "_"
      else name.decodedName.toString
    }
  }
}
