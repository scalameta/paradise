import scala.annotation.StaticAnnotation

class repeatedParam(foos: Any*) extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn
  }
}
