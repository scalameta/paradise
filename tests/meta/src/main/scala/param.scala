import scala.annotation.StaticAnnotation
import scala.meta._

class param(some: String) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val q"new $_($string)" = this
    defn
  }
}
