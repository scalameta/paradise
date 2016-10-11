import java.io._
import scala.compat.Platform.EOL
import scala.io.Source
import scala.tools.nsc.ScalaDoc

class ScaladocSuite extends ToolSuite {
  val resourceDir = new File(
    System.getProperty("sbt.paths.tests.scaladoc") + File.separatorChar + "resources")
  val testDirs = resourceDir
    .listFiles()
    .filter(_.isDirectory)
    .filter(_.listFiles().nonEmpty)
    .filter(!_.getName().endsWith("_disabled"))
  testDirs.foreach(testDir =>
    test(testDir.getName) {
      val (exitCode, output) = runCompiler(testDir, options => ScalaDoc.main(options))
      val actualOutput       = exitCode + EOL + output
      val expectedOutput     = Source.fromFile(testDir.getAbsolutePath + ".check").mkString
      assert(actualOutput === expectedOutput)
  })
}
