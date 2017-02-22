// NOTE: much of this build is copy/pasted from scalameta/scalameta
import java.io._
import scala.util.Try
import org.scalameta.os
import PgpKeys._

lazy val ScalaVersion   = "2.11.8"
lazy val ScalaVersions  = Seq("2.11.8", "2.12.1")
lazy val MetaVersion    = "1.6.0"
lazy val LibrarySeries  = "3.0.0"
lazy val LibraryVersion = computePreReleaseVersion(LibrarySeries)

// ==========================================
// Projects
// ==========================================

lazy val paradiseRoot = Project(
    id = "paradiseRoot",
    base = file(".")
  ) settings (
    sharedSettings,
    commands += Command.command("ci") { state =>
    "very paradiseRoot/test" ::
      state
  },
    packagedArtifacts := Map.empty,
    aggregate in publish := false,
    publish := {
    val publishPlugin = (publish in plugin in Compile).value
  },
    aggregate in publishSigned := false,
    publishSigned := {
    val publishPlugin = (publishSigned in plugin in Compile).value
  },
    aggregate in test := false,
    test := {
    val runMetaTests    = (test in testsMeta in Test).value
    val runReflectTests = (test in testsReflect in Test).value
  }
  ) aggregate (
    plugin,
    testsCommon,
    testsMeta,
    testsReflect
  )

// main scala.meta paradise plugin
lazy val plugin = Project(
    id = "paradise",
    base = file("plugin")
  ) settings (
    publishableSettings,
    mergeSettings,
    libraryDependencies += "org.scalameta"  % "scalahost"      % MetaVersion cross CrossVersion.full,
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value,
    addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
  )

lazy val testsCommon = Project(
    id = "testsCommon",
    base = file("tests/common")
  ) settings (
    sharedSettings,
    publish := {},
    publishSigned := {},
    libraryDependencies += "org.scalatest"  %% "scalatest"     % "3.0.1",
    libraryDependencies += "org.scala-lang" % "scala-compiler" % scalaVersion.value
  )

// macro annotation tests, requires a clean on every compile to outsmart incremental compiler.
lazy val testsReflect = Project(
    id = "testsReflect",
    base = file("tests/reflect")
  ) settings (
    sharedSettings,
    usePluginSettings,
    publish := {},
    publishSigned := {},
    exposePaths("testsReflect", Test)
  ) dependsOn (testsCommon)

// macro annotation tests, requires a clean on every compile to outsmart incremental compiler.
lazy val testsMeta = Project(
    id = "testsMeta",
    base = file("tests/meta")
  ) settings (
    sharedSettings,
    usePluginSettings,
    publish := {},
    publishSigned := {},
    exposePaths("testsMeta", Test)
  ) dependsOn (testsCommon)

// ==========================================
// Settings
// ==========================================

lazy val sharedSettings = Def.settings(
  scalaVersion := ScalaVersion,
  crossScalaVersions := ScalaVersions,
  crossVersion := CrossVersion.full,
  version := LibraryVersion,
  organization := "org.scalameta",
  description := "Empowers production Scala compiler with latest macro developments",
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.bintrayIvyRepo("scalameta", "maven"),
  libraryDependencies += "org.scalameta" %% "scalameta" % MetaVersion,
  libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
  scalacOptions ++= Seq("-deprecation", "-feature", "-unchecked"),
  scalacOptions ++= Seq("-Xfatal-warnings"),
  logBuffered := false,
  // NOTE: sbt 0.13.8 provides cross-version support for Scala sources
  // (http://www.scala-sbt.org/0.13/docs/sbt-0.13-Tech-Previews.html#Cross-version+support+for+Scala+sources).
  // Unfortunately, it only includes directories like "scala_2.11" or "scala_2.12",
  // not "scala_2.11.8" or "scala_2.12.1" that we need.
  // That's why we have to work around here.
  unmanagedSourceDirectories in Compile += {
    val base = (sourceDirectory in Compile).value
    base / ("scala-" + scalaVersion.value)
  },
  unmanagedSourceDirectories in Test += {
    val base = (sourceDirectory in Test).value
    base / ("scala-" + scalaVersion.value)
  },
  triggeredMessage in ThisBuild := Watched.clearWhenTriggered
)

lazy val mergeSettings = Def.settings(
  test in assembly := {},
  logLevel in assembly := Level.Error,
  assemblyMergeStrategy in assembly := {
    // conflicts with scalahost plugin
    case "scalac-plugin.xml" => MergeStrategy.first
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  },
  assemblyJarName in assembly := name.value + "_" + scalaVersion.value + "-" + version.value + "-assembly.jar",
  assemblyOption in assembly ~= { _.copy(includeScala = false) },
  Keys.`package` in Compile := {
    val slimJar = (Keys.`package` in Compile).value
    val fatJar  = new File(crossTarget.value + "/" + (assemblyJarName in assembly).value)
    val _       = assembly.value
    IO.copy(List(fatJar -> slimJar), overwrite = true)
    slimJar
  },
  packagedArtifact in Compile in packageBin := {
    val temp           = (packagedArtifact in Compile in packageBin).value
    val (art, slimJar) = temp
    val fatJar         = new File(crossTarget.value + "/" + (assemblyJarName in assembly).value)
    val _              = assembly.value
    IO.copy(List(fatJar -> slimJar), overwrite = true)
    (art, slimJar)
  }
)

lazy val usePluginSettings = Seq(
  scalacOptions ++= {
    val jar = (Keys.`package` in plugin in Compile).value
    System.setProperty("sbt.paths.plugin.jar", jar.getAbsolutePath)
    val addPlugin = "-Xplugin:" + jar.getAbsolutePath
    // Thanks Jason for this cool idea (taken from https://github.com/retronym/boxer)
    // add plugin timestamp to compiler options to trigger recompile of
    // main after editing the plugin. (Otherwise a 'clean' is needed.)
    val dummy = "-Jdummy=" + jar.lastModified
    Seq(addPlugin, dummy)
  }
)

def computePreReleaseVersion(LibrarySeries: String): String = {
  val preReleaseSuffix = {
    import sys.process._
    val stableSha = Try(os.git.stableSha()).toOption
    val commitSubjects =
      Try(augmentString(os.shell.check_output("git log -10 --pretty=%s", cwd = ".")).lines.toList)
        .getOrElse(Nil)
    val prNumbers = commitSubjects.map(commitSubject => {
      val Merge  = "Merge pull request #(\\d+).*".r
      val Squash = ".*\\(#(\\d+)\\)".r
      commitSubject match {
        case Merge(prNumber)  => Some(prNumber)
        case Squash(prNumber) => Some(prNumber)
        case _                => None
      }
    })
    val mostRecentPrNumber = prNumbers.flatMap(_.toList).headOption
    (stableSha, prNumbers, mostRecentPrNumber) match {
      case (Some(_), Some(prNumber) +: _, _) => prNumber
      case (_, _, Some(prNumber))            => prNumber + "." + System.currentTimeMillis()
      case _                                 => "unknown" + "." + System.currentTimeMillis()
    }
  }
  LibrarySeries + "-" + preReleaseSuffix
}

// Pre-release versions go to bintray and should be published via `publish`.
// This is the default behavior that you get without modifying the build.
// The only exception is that we take extra care to not publish on pull request validation jobs in Drone.
def shouldPublishToBintray: Boolean = {
  if (!new File(sys.props("user.home") + "/.bintray/.credentials").exists) return false
  if (sys.props("sbt.prohibit.publish") != null) return false
  if (sys.env.contains("CI_PULL_REQUEST")) return false
  LibraryVersion.contains("-")
}

// Release versions go to sonatype and then get synced to maven central.
// They should be published via `publish-signed` and signed by someone from <developers>.
// In order to publish a release version, change LibraryVersion to be equal to LibrarySeries.
def shouldPublishToSonatype: Boolean = {
  if (!os.secret.obtain("sonatype").isDefined) return false
  if (sys.props("sbt.prohibit.publish") != null) return false
  !LibraryVersion.contains("-")
}

lazy val publishableSettings = Def.settings(
  sharedSettings,
  bintrayOrganization := Some("scalameta"),
  publishArtifact in Compile := true,
  publishArtifact in Test := false, {
    val publishingStatus = {
      if (shouldPublishToBintray) "publishing to Bintray"
      else if (shouldPublishToSonatype) "publishing to Sonatype"
      else "publishing disabled"
    }
    println(s"[info] Welcome to paradise $LibraryVersion ($publishingStatus)")
    publish in Compile := (Def.taskDyn {
      if (shouldPublishToBintray)
        Def.task { publish.value } else if (shouldPublishToSonatype) Def.task {
        sys.error("Use publish-signed to publish release versions"); ()
      } else Def.task { sys.error("Undefined publishing strategy"); () }
    }).value
  },
  publishSigned in Compile := (Def.taskDyn {
    if (shouldPublishToBintray) Def.task {
      sys.error("Use publish to publish pre-release versions"); ()
    } else if (shouldPublishToSonatype) Def.task { publishSigned.value } else
      Def.task { sys.error("Undefined publishing strategy"); () }
  }).value,
  publishTo := {
    if (shouldPublishToBintray) (publishTo in bintray).value
    else if (shouldPublishToSonatype)
      Some("releases" at "https://oss.sonatype.org/" + "service/local/staging/deploy/maven2")
    else publishTo.value
  },
  credentials ++= {
    if (shouldPublishToBintray) {
      // NOTE: Bintray credentials are automatically loaded by the sbt-bintray plugin
      Nil
    } else if (shouldPublishToSonatype) {
      os.secret
        .obtain("sonatype")
        .map({
          case (username, password) =>
            Credentials("Sonatype Nexus Repository Manager",
                        "oss.sonatype.org",
                        username,
                        password)
        })
        .toList
    } else Nil
  },
  publishMavenStyle := {
    if (shouldPublishToBintray) false
    else if (shouldPublishToSonatype) true
    else publishMavenStyle.value
  },
  pomIncludeRepository := { x =>
    false
  },
  licenses += "BSD" -> url("https://github.com/scalameta/paradise/blob/master/LICENSE.md"),
  pomExtra := (
    <url>https://github.com/scalameta/paradise</url>
    <inceptionYear>2012</inceptionYear>
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
  )
)

def exposePaths(projectName: String, config: Configuration) = {
  def uncapitalize(s: String) =
    if (s.length == 0) ""
    else { val chars = s.toCharArray; chars(0) = chars(0).toLower; new String(chars) }
  val prefix = "sbt.paths." + projectName + "." + uncapitalize(config.name) + "."
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
