import org.scalatest.FunSuite
import scala.annotation.StaticAnnotation

// TODO: DavidDudson: simplify with argument macros
class Expansion extends FunSuite {

  test("Nested macro should expand with identity") {
    @identity
    object Foo {
      @helloWorld def bar() = "test"
    }

    assert(Foo.bar() === "hello world")
  }

  test("Nested macro of the same name should expand") {
    var letters = ""

    @appendA
    def foo() = {
      @appendA
      def bar() = {}
      bar()
    }

    foo()

    assert(letters === "aa")
  }

  test("Nested macro should expand with different macros") {
    var letters = ""

    @appendA
    def foo() = {
      @appendB
      def bar() = {}
      bar()
    }

    foo()

    assert(letters === "ba")
  }

  // Provided the above test passes... This proves that
  // Macros expansion order:
  // Left -> Right
  // In -> Out
  test("Verify expansion order") {
    var letters = ""

    @appendB
    @appendC
    def foo() = {
      @appendA
      def bar() = {}
      bar()
    }

    foo()

    assert(letters === "abc")
  }

  test("Nested macro should expand with inner identity macro") {
    var letters = ""

    @appendA
    def foo() = {
      @identity
      def bar() = {}
      bar()
    }

    foo()

    assert(letters === "a")
  }

  test("Nested macro should expand with outer identity macro") {
    var letters = ""

    @identity
    def foo() = {
      @appendA
      def bar() = {}
      bar()
    }

    foo()

    assert(letters === "a")
  }

  test("Placebo after expandee should compile and work") {
    var letters = ""

    @appendA
    @placebo
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Placebo before expandee should compile and work") {
    var letters = ""

    @placebo
    @appendA
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Multiple expandees of same kinds with others in between should expand") {
    var letters = ""

    @appendA
    @identity
    @appendB
    def bar() = {}

    bar()

    assert(letters === "ab")
  }

  test("Multiple expandees of similar kinds should expand in the correct order") {
    var letters = ""

    @appendA
    @appendB
    def bar() = {}

    bar()

    assert(letters === "ab")
  }

  test("Identity expandee followed by regular expandee should expand correctly") {
    var letters = ""

    @identity
    @appendA
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Regular expandee followed by Identity expandee should expand correctly") {
    var letters = ""

    @appendA
    @identity
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Placebo in package doesn't accidentally get removed if second") {
    var letters = ""

    @appendA
    @placebo.appendA
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Placebo in package doesn't accidentally get removed if first") {
    var letters = ""

    @placebo.appendA
    @appendA
    def bar() = {}

    bar()

    assert(letters === "a")
  }

  test("Regular arguments are supported") {
    @param("hello world")
    class SomeClass1
  }

  test("Named arguments are supported") {
    @namedParam(some = "text")
    class SomeClass2
  }

  test("Repeated arguments are supported") {
    @repeatedParam(foos: _*)
    class SomeClass3
  }

  test("Annotations on classes expand not only classes, but also companion objects") {
    trait Bar {
      val k: Int = 3
    }

    @companion
    class Foo(id: Int) {
      val i: Int = 1
    }

    object Foo extends Bar {
      val j: Int = 2
    }

    @companion
    class Baz(id: Int) {
      val a: String = "abc"
    }
  }

  test("Type parameters are supported") {
    @tparam[Int]
    class Foo
  }

  test("Private in block return") {
    @genLargeNumberOfStats
    class foo
  }

  test("Expansion of type def with companion") {
    object SomeObject {
      @identity type A = Int
      object A
    }
  }

  test("Ensure companion is passed into macro with typedef") {
    object AnotherObject {
      @typeWithCompanion type A = Int
      object A
    }
  }

  test("Ensure typedefs without companions are not in a block") {
    object AThirdObject {
      @typeWithoutCompanion type A = Int
    }
  }
}

// Note: We cannot actually wrap this in test()
// as it cannot be an inner class definition.
// The best we can do is make sure this compiles
// TODO: David Dudson -> Make a more elaborate test case
// eg. A macro that creates a macro that is then used
// requiring 3 seperate compilation steps
@identity
class SomeClass4 extends StaticAnnotation {
  inline def apply(a: Any): Any = meta(a)
}
