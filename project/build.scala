import sbt._
import Keys._
import sbtassembly.Plugin._
import AssemblyKeys._

object build extends Build {
  lazy val ScalaVersions = Seq("2.11.8")
  lazy val MetaVersion = "1.2.0-SNAPSHOT"

  lazy val sharedSettings = Defaults.defaultSettings ++ Seq(
    scalaVersion := ScalaVersions.head,
    crossVersion := CrossVersion.full,
    version := "3.0.0-SNAPSHOT",
    organization := "org.scalameta",
    description := "Empowers production Scala compiler with latest macro developments",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    scalacOptions ++= Seq("-deprecation", "-feature"),
    parallelExecution in Test := false, // hello, reflection sync!!
    logBuffered := false,
    scalaHome := {
      val scalaHome = System.getProperty("paradise.scala.home")
      if (scalaHome != null) {
        println(s"Going for custom scala home at $scalaHome")
        Some(file(scalaHome))
      } else None
    }
  )

  def loadCredentials(): List[Credentials] = {
    val mavenSettingsFile = System.getProperty("maven.settings.file")
    if (mavenSettingsFile != null) {
      println("Loading Sonatype credentials from " + mavenSettingsFile)
      try {
        import scala.xml._
        val settings = XML.loadFile(mavenSettingsFile)
        def readServerConfig(key: String) = (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
        List(Credentials(
          "Sonatype Nexus Repository Manager",
          "oss.sonatype.org",
          readServerConfig("username"),
          readServerConfig("password")
        ))
      } catch {
        case ex: Exception =>
          println("Failed to load Maven settings from " + mavenSettingsFile + ": " + ex)
          Nil
      }
    } else {
      // println("Sonatype credentials cannot be loaded: -Dmaven.settings.file is not specified.")
      Nil
    }
  }

  lazy val root = Project(
    id = "root",
    base = file("root")
  ) settings (
    sharedSettings : _*
  ) settings (
    packagedArtifacts := Map.empty,
    aggregate in test := false,
    test := (test in tests in Test).value
  ) aggregate (
    plugin,
    tests
  )

  lazy val plugin = Project(
    id   = "paradise",
    base = file("plugin")
  ) settings (
    sharedSettings : _*
  ) settings (
    assemblySettings : _*
  ) settings (
    resourceDirectory in Compile <<= baseDirectory(_ / "src" / "main" / "scala" / "org" / "scalameta" / "paradise" / "embedded"),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-library" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies += "org.scalameta" %% "scalameta" % MetaVersion,
    // NOTE: here we depend on the old paradise to build the new one. this is intended.
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    test in assembly := {},
    logLevel in assembly := Level.Error,
    jarName in assembly := name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    Keys.`package` in Compile := {
      val slimJar = (Keys.`package` in Compile).value
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      slimJar
    },
    packagedArtifact in Compile in packageBin := {
      val temp = (packagedArtifact in Compile in packageBin).value
      val (art, slimJar) = temp
      val fatJar = new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      (art, slimJar)
    },
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { x => false },
    pomExtra := (
      <url>https://github.com/scalameta/paradise</url>
      <inceptionYear>2012</inceptionYear>
      <licenses>
        <license>
          <name>BSD-like</name>
          <url>http://www.scala-lang.org/downloads/license.html</url>
          <distribution>repo</distribution>
        </license>
      </licenses>
      <scm>
        <url>git://github.com/scalameta/paradise.git</url>
        <connection>scm:git:git://github.com/scalameta/paradise.git</connection>
      </scm>
      <issueManagement>
        <system>GitHub</system>
        <url>https://github.com/scalameta/paradise/issues</url>
      </issueManagement>
      <developers>
        <developer>
          <id>xeno-by</id>
          <name>Eugene Burmako</name>
          <url>http://xeno.by</url>
        </developer>
      </developers>
    ),
    credentials ++= loadCredentials()
  )

  lazy val usePluginSettings = Seq(
    scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
      System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
      val addPlugin = "-Xplugin:" + jar.getAbsolutePath
      // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
      // add plugin timestamp to compiler options to trigger recompile of
      // main after editing the plugin. (Otherwise a 'clean' is needed.)
      val dummy = "-Jdummy=" + jar.lastModified
      Seq(addPlugin, dummy)
    }
  )

  lazy val tests = Project(
    id   = "tests",
    base = file("tests")
  ) settings (
    sharedSettings ++ usePluginSettings: _*
  ) settings (
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-reflect" % _),
    libraryDependencies <+= (scalaVersion)("org.scala-lang" % "scala-compiler" % _),
    libraryDependencies += "org.scalameta" %% "scalameta" % MetaVersion,
    libraryDependencies += "org.scalatest" % "scalatest_2.11" % "3.0.0" % "test",
    libraryDependencies += "org.scalacheck" % "scalacheck_2.11" % "1.10.2-SNAPSHOT" % "test",
    scalacOptions += "-Ywarn-unused-import",
    scalacOptions += "-Xfatal-warnings",
    publishArtifact in Compile := false,
    unmanagedSourceDirectories in Test <<= (scalaSource in Test) { (root: File) =>
      // TODO: I haven't yet ported negative tests to SBT, so for now I'm excluding them
      val (anns :: Nil, others) = root.listFiles.toList.partition(_.getName == "annotations")
      val oldAnns = anns.listFiles.find(_.getName == "old").get
      val (oldNegAnns, oldOtherAnns) = oldAnns.listFiles.toList.partition(_.getName == "neg")
      val newAnns = anns.listFiles.find(_.getName == "new").get
      val newAllAnns = newAnns.listFiles.toList
      System.setProperty("sbt.paths.tests.scaladoc", oldAnns.listFiles.toList.filter(_.getName == "scaladoc").head.getAbsolutePath)
      others ++ oldOtherAnns ++ newAllAnns
    },
    fullClasspath in Test := {
      val testcp = (fullClasspath in Test).value.files.map(_.getAbsolutePath).mkString(java.io.File.pathSeparatorChar.toString)
      sys.props("sbt.paths.tests.classpath") = testcp
      (fullClasspath in Test).value
    },
    scalacOptions ++= Seq()
    // scalacOptions ++= Seq("-Xprint:typer")
    // scalacOptions ++= Seq("-Xlog-implicits")
  )
}
