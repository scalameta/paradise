import scala.annotation.StaticAnnotation
import scala.meta._

class namedParam(some: String) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val q"new $_(some = $string)" = this
    defn
  }
}
