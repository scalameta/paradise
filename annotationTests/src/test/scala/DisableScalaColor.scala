import org.scalatest._

/** Ensure that scala.color doesn't interact with the REPL output assertions. */
trait DisableScalaColor extends BeforeAndAfterAll { this: Suite =>
  var scalaColor: Option[String] = None

  override protected def beforeAll() = {
    scalaColor = sys.props get "scala.color"
    if (scalaColor.isDefined) sys.props("scala.color") = "false"
    super.beforeAll()
  }

  override protected def afterAll() = {
    super.afterAll()
    scalaColor foreach (s => sys.props("scala.color") = s)
  }
}
