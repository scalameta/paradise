package org.scalameta.paradise
package mirrors

import scala.tools.nsc.Global
import scala.{meta => m}
import org.scalameta.paradise.reflect.ReflectToolkit
import org.scalameta.paradise.converters.Converter

trait Mirrors extends ReflectToolkit with Converter {
  implicit object mirror extends m.Mirror {
    def tpe(member: m.Member): m.Completed[m.Type] = apiBoundary {
      ???
    }

    def desugar(tree: m.Tree): m.Completed[m.Tree] = apiBoundary {
      ???
    }

    private def apiBoundary[T](body: => T): m.Completed[T] = {
      try {
        m.Completed.Success(body)
      } catch {
        case NonFatal(ex: Exception) =>
          m.Completed.Error(ex)
      }
    }
  }
}

object Mirrors {
  def apply[G <: Global](global0: G): Mirrors { val global: G } = {
    new Mirrors { val global: G = global0 }
  }
}
