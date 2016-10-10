package scala.meta

import java.io.File
import java.nio.file.Paths

import org.scalatest.FunSuite

class DeserializeSpec extends FunSuite {

  test("deserialize TASTY section in .class file") {
    val workingDir = {
      val wd = new File(System.getProperty("user.dir"))
      if (wd.getName == "semanticTests") wd.getParent
      else wd.getAbsolutePath
    }
    val path =
      Paths
        .get(workingDir, "semanticCompile", "target")
        .toAbsolutePath
        .toString
    val mirror = Mirror(Artifact(path))
//    assert(mirror.domain.sources.nonEmpty) // no semantic tests yet.
  }
}
