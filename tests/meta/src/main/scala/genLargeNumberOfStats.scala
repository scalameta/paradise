import scala.meta._

class genLargeNumberOfStats {
  // Not supported
  // private[foo] val baz = 2;
  inline def apply(tree: Any): Any = meta {
    q"""private class Foo;
        protected final def bar = 2;
        trait Bar;
        abstract class Baz;
        object Baq;
        type Ban = Int
      """
  }
}
