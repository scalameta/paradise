class Syntactic extends ConverterSuite {
  syntactic("case class C()")
  syntactic("object M { override val toString = test5 }")
  syntactic("foo(named = arg)")
  syntactic("""
    1 match {
      case 0 | 1           => true
      case (2 | 3 | 4 | 5) => false
    }
  """)
  syntactic("def add(a: Int)(implicit z: Int = 0) = a + z")
}