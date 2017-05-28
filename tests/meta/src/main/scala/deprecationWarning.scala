import scala.annotation.StaticAnnotation
import scala.meta._

class deprecationWarning extends StaticAnnotation {
  inline def apply(tree: Any): Any = meta {
    Term.Block(q"println(2)" :: q"println(2)" :: Nil)
  }
}

