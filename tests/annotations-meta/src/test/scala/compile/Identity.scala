@identity
case class Test1()

@identity()
case class Test2()

@identity
case class Test3()

@identity()
case class Test4()

@identity
object Test5 {
  override val toString = "Test5"
}

@identity
object NamedArg {
  case class MyCaseClass(a: Int, b: Int)
  MyCaseClass(0, 1).copy(2, b = 3)
}

@identity
object PatAlternative {
  1 match {
    case 0 | 1           => true
    case (2 | 3 | 4 | 5) => false
  }
}

@identity
object ImplicitArg {
  def add(a: Int)(implicit z: Int = 0) = a + z
}
