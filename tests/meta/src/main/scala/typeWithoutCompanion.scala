import scala.annotation.StaticAnnotation
import scala.meta._

class typeWithoutCompanion extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case t: Defn.Type => t
      case _ =>
        abort("Type should be passed in by itself")
    }
  }
}