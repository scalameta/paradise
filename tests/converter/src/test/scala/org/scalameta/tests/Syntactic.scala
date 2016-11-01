package org.scalameta.tests

class Playground extends ConverterSuite {
}
// NOTE: a lot of these tests are taken from https://github.com/liufengyun/eden/blob/master/src/test/scala/dotty/eden/UntpdSuite.scala
class Syntactic extends ConverterSuite {
  // terms
  syntactic("null")
  syntactic("""println("hello, world")""")
  syntactic("println(42)")
  syntactic("f(this)")
  syntactic("f(A.this)")
  syntactic("this.age")
  syntactic("C.this.age")
  syntactic("super.age")
  syntactic("super[A].age")
  syntactic("C.super[A].age")
  syntactic("f[Int](3)")
  syntactic("f(x = 3)")
  syntactic("f(x = 3, y = 6 * 8)")
  syntactic("f(x:_*)")
  syntactic("a.f(this.age)")
  syntactic("a + b")
  syntactic("a.+(b)")
  syntactic("a + b + c + this.age")
  syntactic("a :+ b")
  syntactic("a.:+(b)")
  syntactic("a :+ (b, c)")
  syntactic("a :+[Int] b")
  syntactic("a.:+[Int](b)")
  syntactic("a :+[Int] (b, c)")
  syntactic("a +: b")
  syntactic("a.+:(b)")
  syntactic("a +: (b, c)")
  syntactic("a +:[Int] b")
  syntactic("a.+:[Int](b)")
  syntactic("a +:[Int] (b, c)")
  syntactic("a*")
  syntactic("++a")
  syntactic("a++")
  syntactic("a = b")
  syntactic("{ a = 1; b += 2 }")
  syntactic("{ }")
  syntactic("()")
  syntactic("(2)")
  syntactic("(2, 4)")
  syntactic("a -> b")
  syntactic("if (cond) a else b")
  syntactic("if (cond) return a")
  syntactic("x.map { case 1 => 2 }")
  syntactic("while (a > 5) { println(a); a++; }")
  syntactic("do { println(a); a++; } while (a > 5)")
  syntactic("return a")
  syntactic("new List(5)")
  syntactic("new List[Int](5)")
  syntactic("new List[List[Int]](List(5))")
  syntactic("new Map[Int, String]")
  syntactic("new Map[Int, String]()")
  syntactic("new Map[Int, String](a -> b)")
  syntactic("new B")
  syntactic("new B()")
  syntactic("new c.B")
  syntactic("new C#B")
  syntactic("new o.C#B")
  syntactic("new B { }")
  syntactic("new B { val a = 3 }")
  syntactic("new B { def f(x: Int): Int = x*x }")
  syntactic("new B(3) { println(5); def f(x: Int): Int = x*x }")
  syntactic("a.foreach(_ => bar())")
  syntactic("a.foreach(_ + _)")
  syntactic("function _")
  syntactic("throw new A(4)")
  syntactic("try { throw new A(4) } catch { case _: Throwable => 4 } finally { println(6) }")
  syntactic("try f(4) catch { case _: Throwable => 4 } finally println(6)")
  syntactic("try f(4) catch { case _: Throwable => 4 }")
  syntactic("try f(4) finally println(6)")
  syntactic("try {} finally println(6)")
  // TODO: https://github.com/scalameta/paradise/issues/75
  // syntactic("try foo catch bar")
  // TODO: https://github.com/scalameta/paradise/issues/74
  // syntactic("for (arg <- args) result += arg * arg")
  // syntactic("for (arg <- args; double = arg * 2) result += arg * arg")
  // syntactic("""
  //   for { i<-1 until n
  //         j <- 1 until i
  //         if isPrime(i+j) } yield (i, j)
  // """)
  // syntactic("""
  //   for { i<-1 until n
  //         j <- 1 until i
  //         k = i + j
  //         if isPrime(i+j) } yield (i, j)
  // """)

  // interpolation
  // TODO: https://github.com/scalameta/paradise/issues/76
  // syntactic("""s"hello, $world"""")
  // syntactic("""s"hello, $world, ${1 + 2}"""")

  // patterns
  syntactic("val _ = a")
  syntactic("a match { case 5 => ; case 6 => }")
  syntactic("a match { case Some(x) => x; case None => y }")
  syntactic("a match { case Some(x) => x; case _ => y }")
  syntactic("a match { case m @ Some(x) => x; case _ => y }")
  syntactic("a match { case m @ Some(t @ Some(x)) => x; case _ => y }")
  syntactic("a match { case m : Int => x; case _ => y }")
  syntactic("a match { case Some(x: Int) | Some(x: String) => x; case _ => y }")
  syntactic("a match { case Some(x: Int) | Some(x: String) | Some(x: Boolean) => x; case _ => y }")
  syntactic("a match { case Some(x: Int) | Some(x: String) | x: Boolean => x; case _ => y }")
  syntactic("x match { case List(head, _*) => x }")
  syntactic("a match { case (x, y) => }")
  syntactic("a match { case x @ _ => }")
  syntactic("a match { case x @ (_: T) => }")
  syntactic("a match { case c: Class[_] => c }")
  syntactic("a match { case c: Class[T] => c }")

  // declarations
  syntactic("val x: Int")
  syntactic("var x: Int")

  // definitions
  syntactic("class Y(x: Int) { def this() = this(1); val x = 2 }")
  syntactic("class Y(x: Int) { def this() = this(1); def this(y: String) = this(y.length) }")
  syntactic("class Y(x: Int) { def this() = this()(1) }")
  syntactic("var x: Int = _")
  syntactic("type Age = Int")
  syntactic("type Age")
  syntactic("type Age >: Int <: Any")
  syntactic("type Age <: Int + Boolean")
  syntactic("type Container[T]")
  syntactic("type Container[T] = List[T]")
  syntactic("type Container[T] <: List[T] { def isEmpty: Boolean; type M }")
  syntactic("type Container[T] <: List[T] with Set[T] { def isEmpty: Boolean; type M }")
  syntactic("type Container[T] <: List[T] with Set[T] { def isEmpty: Boolean; type M = Int }")
  syntactic("type Container[T] <: List[T] with Set[T] { def isEmpty: Boolean; type M <: Int }")

  // types
  syntactic("val a: A with B = ???")
  syntactic("val a: A { def x: Int } = ???")
  syntactic("val a: A with B { def x: Int } = ???")
  syntactic("val a: A {} = ???")
  syntactic("val a: A with B {} = ???")
  syntactic("val a: {} = ???")
  syntactic("val a: m.A = ???")
  syntactic("val a: m.d.e.A = ???")
  syntactic("val a: m.List[m.t.A] = ???")
  syntactic("val a: A#B = ???")
  syntactic("val a: m.A#B = ???")
  syntactic("val a: m.A#B#C = ???")
  syntactic("(f: (A => B) => (C, D) => D)")
  syntactic("(f: ((A, A) => B) => (C, D) => D)")
  syntactic("val a: o.type = ???")
  syntactic("val a: o.x.type = ???")
  syntactic("val a: A | B = ???")
  syntactic("val a: |[A, B] = ???")
  syntactic("val a: A | B | C = ???")
  syntactic("val a: A & B & C = ???")
  syntactic("val a: M[A + (B, C)] = ???")
  syntactic("val a: M[(A)] = ???")
  syntactic("val a: Option[(Int, String)] = ???")
  syntactic("val a: T forSome { type T } = ???")
  syntactic("trait Service[F[_]]")

  // imports
  syntactic("import a._")
  syntactic("import a.b")
  syntactic("import a.{b => b}")
  syntactic("import a.{b => c, d => e}")
  syntactic("import a.{b => c, _}")
  syntactic("import a.{b => _}")
  syntactic("import a.{b => _, c => _, d, _}")
  syntactic("import a.{b => c, _ => _}")
  // TODO: fixup in ConverterSuite.scala
  // syntactic("import a.b, a.c")

  // annotation
  syntactic("@static val a: m.A = ???")
  syntactic("@static() val a: m.A = ???")
  syntactic("@static() @volatile() val a: m.A = ???")
  syntactic("@main class App { println(100) }")
  syntactic("@main() class App { println(100) }")
  syntactic("@optimize(5) def fact(n: Int) = ???")
  syntactic("@optimize(5) @log(3) def fact(n: Int) = ???")
  syntactic("(x: @unchecked)")
  syntactic("(x: @unchecked())")
  syntactic("(x: @unchecked @optimize)")
  syntactic("(x: @unchecked() @optimize(3))")
  syntactic("(x: @unchecked()): @optimize(3)")
  syntactic("(x: @unchecked() @optimize(3) @bar)")
  syntactic("((x: @unchecked()).foo: @optimize(3))")
  syntactic("trait Foo[-T] extends Comparator[T @uncheckedVariance]")
  syntactic("trait Foo[-T] extends Comparator[T @uncheckedVariance()]")
  syntactic("trait Foo[-T] extends Comparator[T @uncheckedVariance() @annot(4)]")
  syntactic("trait Function0[@specialized(Unit, Int, Double) T]")
  syntactic("x match { case m: Type[Any] @unchecked => m }")

  // bounds
  syntactic("class f[T <% A](x: T)")
  syntactic("class f[T: A](x: T)")
  syntactic("def f[T <% A: B](x: T): Int = x")
  syntactic("def f[T <% A](x: T): Int = x")
  syntactic("def f[T: A](x: T): Int = x")
  syntactic("def f[T: A :B](x: T): Int = x")
  syntactic("def f[T: A[Int]](x: T): Int = x")

  // pkg
  syntactic("package object a { val x = 1 }")
  syntactic("package a.b { }")

  // random stuff
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
  syntactic("def f(x: => T) = ???")
}
