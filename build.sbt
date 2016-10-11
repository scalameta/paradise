import sbtassembly.Plugin._
import AssemblyKeys._

lazy val ScalaVersions = Seq("2.11.8")
// TODO(olafur) change to org.scalameta:scalameta:2.0 once it gets non-snapshot releases
// I (olafur) published scala.meta 2.0 non-shapshot versions from com.geirsson
// because I don't want to wait ~30 on every single SBT reload to resolve SNAPSHOT
// versions of scala.meta 2.0. The releases are on Maven:
//   http://search.maven.org/#artifactdetails%7Ccom.geirsson%7Cscalameta_2.11%7C2.0.0-M6%7Cjar
lazy val MetaOrg     = "com.geirsson"
lazy val MetaVersion = "2.0.0-M6"

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
      resolvers += Resolver.sonatypeRepo("releases"),
      publishMavenStyle := true,
      publishArtifact in Test := false,
      scalacOptions ++= Seq("-deprecation", "-feature"),
      parallelExecution in Test := false, // hello, reflection sync!!
      logBuffered := false,
      triggeredMessage in ThisBuild := Watched.clearWhenTriggered,
      scalaHome := {
        val scalaHome = System.getProperty("paradise.scala.home")
        if (scalaHome != null) {
          println(s"Going for custom scala home at $scalaHome")
          Some(file(scalaHome))
        } else None
      }
    )

lazy val usePluginSettings: Seq[Def.Setting[_]] = Seq(
  scalacOptions in Compile <++= (Keys.`package` in (plugin, Compile)) map { (jar: File) =>
    System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(
      addPlugin,
      "-Dpersist.enable",
      "-Ybackend:GenBCode",
      "-Dtasty.debug",
      dummy
    )
  }
)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  libraryDependencies += MetaOrg          %% "scalameta"     % MetaVersion,
  libraryDependencies += "org.scala-lang" % "scala-reflect"  % scalaVersion.value,
  libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
  libraryDependencies += "org.scalatest"  % "scalatest_2.11" % "3.0.0" % "test",
  scalacOptions += "-Ywarn-unused-import",
  scalacOptions += "-Xfatal-warnings",
  publishArtifact in Compile := false,
  scalacOptions in Compile ++= Seq()
  // scalacOptions ++= Seq("-Xprint:typer")
  // scalacOptions ++= Seq("-Xlog-implicits")
)

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
    packagedArtifacts := Map.empty
  )
  .aggregate(
    plugin,
    annotationTests,
    syntacticTests,
    semanticCompile,
    semanticTests
  )

// main scala.meta paradise plugin
lazy val plugin = Project(id = "paradise", base = file("plugin"))
  .settings(
    sharedSettings,
    assemblySettings,
    resourceDirectory in Compile <<=
      baseDirectory(_ / "src" / "main" / "scala" / "org" / "scalameta" / "paradise" / "embedded"),
    libraryDependencies ++= Seq(
      MetaOrg          %% "scalameta"     % MetaVersion,
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
    publishArtifact in Test := false,
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

// project for syntactic API tests
lazy val syntacticTests = project
  .settings(
    sharedSettings,
    usePluginSettings,
    testSettings
  )
  .dependsOn(plugin)

// project to generate .class files with TASTY sections, requires `sbt -Dpersist.enable`
lazy val semanticCompile = project
  .settings(
    sharedSettings,
    usePluginSettings,
    testSettings
  )

// project to consume .class files with TASTY sections from semanticCompile
lazy val semanticTests = project
  .settings(
    sharedSettings,
    testSettings,
    fork := true // required to de-serialize TASTY segments in .class files.
  )
  .dependsOn(plugin)

// macro-annotation tests, requires a clean on every compile to outsmart incremental compiler.
lazy val annotationTests = project
  .settings(
    sharedSettings,
    usePluginSettings,
    unmanagedSourceDirectories in Test <<= (scalaSource in Test) { (root: File) =>
      // TODO: I haven't yet ported negative tests to SBT, so for now I'm excluding them
      val (anns :: Nil, others) =
        root.listFiles.toList.partition(_.getName == "annotations")
      val oldAnns = anns.listFiles.find(_.getName == "old").get
      val (oldNegAnns, oldOtherAnns) =
        oldAnns.listFiles.toList.partition(_.getName == "neg")
      val newAnns    = anns.listFiles.find(_.getName == "new").get
      val newAllAnns = newAnns.listFiles.toList
      System.setProperty("sbt.paths.tests.scaladoc",
                         oldAnns.listFiles.toList
                           .filter(_.getName == "scaladoc")
                           .head
                           .getAbsolutePath)
      others ++ oldOtherAnns ++ newAllAnns
    },
    fullClasspath in Test := {
      val testcp = (fullClasspath in Test).value.files
        .map(_.getAbsolutePath)
        .mkString(java.io.File.pathSeparatorChar.toString)
      sys.props("sbt.paths.tests.classpath") = testcp
      (fullClasspath in Test).value
    },
    testSettings
  )
