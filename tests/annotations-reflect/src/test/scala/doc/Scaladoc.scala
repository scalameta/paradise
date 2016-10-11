import java.io._
import scala.compat.Platform.EOL
import scala.io.Source
import scala.tools.nsc.ScalaDoc

class Scaladoc extends ToolSuite("testsAnnotationsReflect") {
  val resourceDir = new File(System
    .getProperty("sbt.paths.testsAnnotationsReflect.test.resources") + File.separatorChar + "doc")
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
