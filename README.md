# Scalameta Paradise Plugin

[![Join the chat at https://gitter.im/scalameta/scalameta](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/scalameta/scalameta?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This is a prototype of new-style ("inline") macros based on scala.meta.
Results can be reproduced with scala.meta 1.1.0 and macro paradise 3.0.0-M5.

```
$ cat Macros.scala
import scala.meta._

class main extends scala.annotation.StaticAnnotation {
  inline def apply(defn: Any) = meta {
    val q"object $name { ..$stats }" = defn
    val main = q"""
      def main(args: Array[String]): Unit = { ..$stats }
    """
    q"object $name { $main }"
  }
}

$ scalac -cp <scala.meta> -Xplugin:<paradise> Macros.scala

$ cat Test.scala
@main object Test {
  println("hello world")
}

$ scalac -cp . -Xplugin:<paradise> Test.scala && scala Test
hello world
```

Check out Eugene Burmako's talk at ScalaDays Berlin 2016 to learn more:
[http://scalamacros.org/paperstalks/2016-06-17-Metaprogramming20.pdf](http://scalamacros.org/paperstalks/2016-06-17-Metaprogramming20.pdf).
