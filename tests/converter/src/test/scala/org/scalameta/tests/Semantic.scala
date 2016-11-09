package org.scalameta.tests

class Semantic extends ConverterSuite {
  semantic("class C { def x = 42 }")
  semantic("class C { def x = 43 }")
}
