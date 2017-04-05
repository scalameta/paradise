import org.scalatest._
import java.io._
import java.nio.charset.Charset
import java.security.Permission

class ToolSuite(project: String) extends FunSuite {
  private def virtualizedPopen(input: String, body: => Unit): (Int, String) = {
    val inputStream = new ByteArrayInputStream(input.getBytes(Charset.forName("UTF-8")))
    val outputStorage = new ByteArrayOutputStream()
    val outputStream = new PrintStream(outputStorage)
    case class SystemExitException(exitCode: Int) extends SecurityException
    val manager = System.getSecurityManager()
    System.setSecurityManager(new SecurityManager {
      override def checkPermission(permission: Permission): Unit = ()
      override def checkPermission(permission: Permission, context: AnyRef): Unit = ()
      override def checkExit(exitCode: Int): Unit = throw new SystemExitException(exitCode)
    })
    try {
      scala.Console.withIn(inputStream)(
        scala.Console.withOut(outputStream)(scala.Console.withErr(outputStream)(body)));
      throw new Exception("failed to capture exit code")
    } catch {
      case SystemExitException(exitCode) =>
        outputStream.close(); (exitCode, outputStorage.toString)
      case ReplExit(exitCode) =>
        outputStream.close(); (exitCode, outputStorage.toString)
    } finally System.setSecurityManager(manager)
  }

  private def commonOptions: List[String] = {
    val cp = List("-cp", sys.props("sbt.paths." + project + ".test.classes"))
    val paradise =
      List("-Xplugin:" + sys.props("sbt.paths.plugin.jar"), "-Xplugin-require:macroparadise")
    val tempDir = File.createTempFile("temp", System.nanoTime.toString); tempDir.delete();
    tempDir.mkdir()
    val output = List("-d", tempDir.getAbsolutePath)
    cp ++ paradise ++ output
  }

  def runCompiler(sourceDir: File, tool: Array[String] => Unit): (Int, String) = {
    val sources =
      sourceDir.listFiles().filter(_.getName.endsWith(".scala")).map(_.getAbsolutePath).toList
    val options = commonOptions ++ sources
    virtualizedPopen("", tool(options.toArray))
  }

  def runRepl(input: String, tool: Array[String] => Unit): (Int, String) = {
    val options = commonOptions
    virtualizedPopen(input, tool(options.toArray))
  }
}
