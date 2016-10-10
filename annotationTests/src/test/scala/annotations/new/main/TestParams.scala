package mainWithParams

@mainWithParams("hello world")
object Test {
  println("bye-bye world")
}

@namedParam(some = "text")
class SomeClass1

@argRepeated(foos: _*)
class SomeClass2