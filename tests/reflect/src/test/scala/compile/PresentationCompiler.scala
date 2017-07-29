import org.scalatest._
import org.ensime.pcplod._

import Matchers._

class PresentationCompiler extends FlatSpec {

  "Presentation Compiler" should "typecheck a compilable file" in withMrPlod("com/acme/foo.scala") { mr =>
    mr.symbolAtPoint('foo) shouldBe Some("com.acme.Foo")
    mr.typeAtPoint('foo) shouldBe Some("com.acme.Foo.type")

    mr.symbolAtPoint('bar) shouldBe Some("com.acme.Foo.bar")
    mr.typeAtPoint('bar) shouldBe Some("Int")

    mr.symbolAtPoint('a) shouldBe Some("com.acme.Foo.a")
    mr.typeAtPoint('a) shouldBe Some("Int")

    mr.messages shouldBe empty
  }

}
