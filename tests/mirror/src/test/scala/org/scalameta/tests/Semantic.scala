package org.scalameta.tests

class Semantic extends ConverterSuite("testsMirror") {
  semantic("class C { def x = 42 }") { c =>
    assert(c.tpe("x") === "Int")
  }
}
