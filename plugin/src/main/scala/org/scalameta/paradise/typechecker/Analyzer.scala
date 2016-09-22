// NOTE: has to be this package or otherwise we won't be able to access private[typechecker] methods
package scala.tools.nsc.typechecker

import scala.tools.nsc.Global
import scala.tools.nsc.typechecker.{Analyzer => NscAnalyzer}
import org.scalameta.paradise.reflect.ReflectToolkit

trait ParadiseAnalyzer extends NscAnalyzer with ReflectToolkit {
  val global: Global
  import global._
  import definitions._
  import paradiseDefinitions._

  override def newTyper(context: Context) = new ParadiseTyper(context)
  class ParadiseTyper(context0: Context) extends Typer(context0) {
    override def typedDefDef(ddef: DefDef): DefDef = {
      val ddef1 = super.typedDefDef(ddef)
      if (ddef1.symbol.hasAnnotation(MetaInlineClass) && !ddef1.symbol.owner.isNewMacroAnnotation) {
        typer.context.error(ddef1.pos, "implementation restriction: inline methods can only be used to define new-style macro annotations")
      }
      ddef1
    }
  }
}