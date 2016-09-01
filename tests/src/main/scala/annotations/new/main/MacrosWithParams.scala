package mainWithParams

import scala.annotation.compileTimeOnly
import scala.meta._

@compileTimeOnly("@mainWithParams not expanded")
class mainWithParams(greeting: String) extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any) = meta {
    val q"new $_($greeting)" = this
    val q"object $name { ..$stats }" = defn
    val main = q"""
      def main(args: Array[String]): Unit = {
        println($greeting)
        ..$stats
      }
    """
    q"object $name { $main }"
  }
}