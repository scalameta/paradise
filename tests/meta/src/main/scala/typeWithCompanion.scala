import scala.annotation.StaticAnnotation
import scala.meta._

class typeWithCompanion extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    defn match {
      case Term.Block((t @ Defn.Type(_, _, _, _)) :: (o @ Defn.Object(_, _, _)) :: Nil) =>
        Term.Block(t :: o :: Nil)
      case t => abort(s"Type should have companion\n ${t.structure}")
    }
  }
}
