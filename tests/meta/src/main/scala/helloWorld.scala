import scala.annotation.StaticAnnotation
import scala.meta._

class helloWorld extends StaticAnnotation {
  inline def apply(defn: Any): Any = meta {
    val q"..$mods def $name[..$tparams](...$paramss): $tpeopt = $expr" = defn
    q"""..$mods def $name[..$tparams](...$paramss): $tpeopt = "hello world""""
  }
}