import scala.annotation.StaticAnnotation

class identity extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn
  }
}
