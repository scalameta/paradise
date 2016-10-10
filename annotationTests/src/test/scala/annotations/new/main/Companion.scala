package main

trait Bar {
  val k: Int = 3
}

@classMacro
class Foo(id: Int) {
  val i: Int = 1
}

object Foo extends Bar {
  val j: Int = 2
}

@classMacro
class Baz(id: Int) {
  val a: String = "abc"
}
