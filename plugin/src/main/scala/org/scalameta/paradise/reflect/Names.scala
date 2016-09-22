package org.scalameta.paradise
package reflect

trait Names {
  self: ReflectToolkit =>

  import global._

  implicit class XtensionInlineManglingName(name: Name) {
    def inlineModuleName: TermName = TermName(name.toString.inlineModuleName)
    def inlineImplName: TermName = TermName(name.toString.inlineImplName)
  }

  implicit class XtensionInlineManglingString(s: String) {
    def inlineModuleName: String = s.toString + "$inline"
    def inlineImplName: String = s.toString
  }

  lazy val InlinePrefixParameterName = TermName("prefix")
  lazy val InlineAnnotationMethodName = TermName("apply")
}
