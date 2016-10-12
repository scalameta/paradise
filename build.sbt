import sbtassembly.Plugin._
import AssemblyKeys._

lazy val ScalaVersions = Seq("2.11.8")
lazy val MetaVersion   = "2.0.0-SNAPSHOT"

// ==========================================
// Settings
// ==========================================

lazy val sharedSettings: Seq[Def.Setting[_]] =
  Defaults.defaultSettings ++
    Seq(
      scalaVersion := ScalaVersions.head,
      crossVersion := CrossVersion.full,
      version := "4.0.0-SNAPSHOT",
      organization := "org.scalameta",
      description := "Empowers production Scala compiler with latest macro developments",
      resolvers += Resolver.sonatypeRepo("snapshots"),
      resolvers += Resolver.sonatypeRepo("releases"),
      publishMavenStyle := true,
      publishArtifact := false,
      scalacOptions ++= Seq("-deprecation", "-feature"),
      logBuffered := false,
      triggeredMessage in ThisBuild := Watched.clearWhenTriggered
    )

lazy val usePluginSettings: Seq[Def.Setting[_]] = Seq(
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

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  libraryDependencies += "org.scalameta"  %% "scalameta"     % MetaVersion,
  libraryDependencies += "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies += "org.scalatest"  % "scalatest_2.11" % "3.0.0",
  scalacOptions += "-Ywarn-unused-import",
  scalacOptions += "-Xfatal-warnings",
  scalacOptions in Compile ++= Seq()
)

def exposePaths(projectName: String, config: Configuration) = {
  def uncapitalize(s: String) =
    if (s.length == 0) ""
    else { val chars = s.toCharArray; chars(0) = chars(0).toLower; new String(chars) }
  val prefix         = "sbt.paths." + projectName + "." + uncapitalize(config.name) + "."
  Seq(
    sourceDirectory in config := {
      val defaultValue = (sourceDirectory in config).value
      System.setProperty(prefix + "sources", defaultValue.getAbsolutePath)
      defaultValue
    },
    resourceDirectory in config := {
      val defaultValue = (resourceDirectory in config).value
      System.setProperty(prefix + "resources", defaultValue.getAbsolutePath)
      defaultValue
    },
    fullClasspath in config := {
      val defaultValue = (fullClasspath in config).value
      val classpath    = defaultValue.files.map(_.getAbsolutePath)
      val scalaLibrary = classpath.map(_.toString).find(_.contains("scala-library")).get
      System.setProperty("sbt.paths.scalalibrary.classes", scalaLibrary)
      System.setProperty(prefix + "classes", classpath.mkString(java.io.File.pathSeparator))
      defaultValue
    }
  )
}

def loadCredentials(): List[Credentials] = {
  val mavenSettingsFile = System.getProperty("maven.settings.file")
  if (mavenSettingsFile != null) {
    println("Loading Sonatype credentials from " + mavenSettingsFile)
    try {
      import scala.xml._
      val settings = XML.loadFile(mavenSettingsFile)
      def readServerConfig(key: String) =
        (settings \\ "settings" \\ "servers" \\ "server" \\ key).head.text
      List(
        Credentials(
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

// ==========================================
// Projects
// ==========================================

lazy val root = project
  .in(file("."))
  .settings(
    sharedSettings,
    packagedArtifacts := Map.empty,
    aggregate in publish := false,
    publish := {
      val publishPlugin = (publish in plugin).value
    }
  )
  .aggregate(
    plugin,
    testsAnnotationsMeta,
    testsAnnotationsReflect
  )

// main scala.meta paradise plugin
lazy val plugin = Project(id = "paradise", base = file("plugin"))
  .settings(
    sharedSettings,
    assemblySettings,
    resourceDirectory in Compile <<=
      baseDirectory(_ / "src" / "main" / "scala" / "org" / "scalameta" / "paradise" / "embedded"),
    libraryDependencies ++= Seq(
      "org.scalameta"  %% "scalameta"     % MetaVersion,
      "org.scala-lang" % "scala-library"  % scalaVersion.value,
      "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    ),
    // NOTE: here we depend on the old paradise to build the new one. this is intended.
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
    test in assembly := {},
    logLevel in assembly := Level.Error,
    jarName in assembly := name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
    assemblyOption in assembly ~= { _.copy(includeScala = false) },
    Keys.`package` in Compile := {
      val slimJar = (Keys.`package` in Compile).value
      val fatJar =
        new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      slimJar
    },
    packagedArtifact in Compile in packageBin := {
      val temp           = (packagedArtifact in Compile in packageBin).value
      val (art, slimJar) = temp
      val fatJar =
        new File(crossTarget.value + "/" + (jarName in assembly).value)
      val _ = assembly.value
      IO.copy(List(fatJar -> slimJar), overwrite = true)
      (art, slimJar)
    },
    publishMavenStyle := true,
    publishArtifact in Compile := true,
    publishTo <<= version { v: String =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { x =>
      false
    },
    pomExtra := (
      <url>https://github.com/scalameta/paradise</url>
              <inceptionYear>2012</inceptionYear>
              <licenses>
                <license>
                  <name>BSD 3-Clause</name>
                  <url>https://github.com/scalameta/paradise/blob/master/LICENSE.md</url>
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

lazy val testsCommon = project
  .in(file("tests/common"))
  .settings(
    sharedSettings,
    testSettings
  )

// macro annotation tests, requires a clean on every compile to outsmart incremental compiler.
lazy val testsAnnotationsReflect = project
  .in(file("tests/annotations-reflect"))
  .settings(
    sharedSettings,
    usePluginSettings,
    testSettings,
    exposePaths("testsAnnotationsReflect", Test)
  )
  .dependsOn(testsCommon)

// macro annotation tests, requires a clean on every compile to outsmart incremental compiler.
lazy val testsAnnotationsMeta = project
  .in(file("tests/annotations-meta"))
  .settings(
    sharedSettings,
    usePluginSettings,
    testSettings,
    exposePaths("testsAnnotationsMeta", Test)
  )
  .dependsOn(testsCommon)
