package org.scalameta.tests

class Semantic extends ConverterSuite("testsMirror") {
  semantic("class C { def x = 42 }") { c =>
    assert(c.tpe("x") === "Int")
  }

  semantic("class C { List(1, 2, 3) }") { c =>
    assert(c.desugar("List") === "immutable.this.List.apply[Int]")
    assert(c.desugar("List(1, 2, 3)") === "immutable.this.List.apply[Int](1, 2, 3)")
  }
}
