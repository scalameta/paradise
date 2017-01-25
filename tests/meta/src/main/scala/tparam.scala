import scala.annotation.StaticAnnotation

class tparam[T] extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    assert(T.toString == "Int")
    defn
  }
}
