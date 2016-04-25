import io.noplay.sbt.SbtCommunitySettings.autoImport._
import io.alphard.sbt._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {

  //////////
  // CORE //
  //////////

  val alphardSbtVersion = "3.28.0"
  val alphardSbtCore = "io.alphard" %% "alphard-sbt-core" % alphardSbtVersion

  /////////
  // JVM //
  /////////

  val alphardSbtJvm = "io.alphard" %% "alphard-sbt-jvm" % alphardSbtVersion

  /////////
  // WEB //
  /////////

  val alphardSbtWeb = "io.alphard" %% "alphard-sbt-web" % alphardSbtVersion

  val sbtRjsVersion = "1.0.7"
  val sbtRjs = "com.typesafe.sbt" %% "sbt-rjs" % sbtRjsVersion

  val sbtLessVersion = "1.1.1"
  val sbtLess = "com.typesafe.sbt" %% "sbt-less" % sbtLessVersion

  val sbtSassVersion = "0.9.3"
  val sbtSass = "org.madoushi.sbt" % "sbt-sass" % sbtSassVersion
}

object Build
  extends Build {

  import BuildDependencies._

  def noplaySbtProject(name: String, isRoot: Boolean = false, overrideScalaVersion: Boolean = true): Project =
    noplayProject("sbt-" + name, isRoot, overrideScalaVersion)
      .settings(
        sbtPlugin := true,
        resolvers ++= Seq(
          "Alphard Maven Public Repository" at "http://repository.alphard.io/maven/public/releases",
          "NoPlay Maven Public Repository" at "http://repository.noplay.io/maven/public/releases",
          Resolver.url("Alphard Ivy Public Releases Repository", url("http://repository.alphard.io/ivy/public/releases"))(Resolver.ivyStylePatterns),
          Resolver.url("NoPlay Ivy Public Releases Repository", url("http://repository.noplay.io/ivy/public/releases"))(Resolver.ivyStylePatterns)
        )
      )
      .enablePlugins(SbtScripted)

  val core =
    noplaySbtProject("core")
      .settings(
        libraryDependencies ++= {
          val sbtBV = sbtBinaryVersion.value
          val scalaBV = scalaBinaryVersion.value
          Seq(
            // web translate it
            "org.json4s" %% "json4s-native" % "3.2.11",
            "org.json4s" %% "json4s-jackson" % "3.2.11",
            "org.joda" % "joda-convert" % "1.8",
            sbtPluginExtra(alphardSbtCore, sbtBV, scalaBV)
          )
        }
      )

  val jvm =
    noplaySbtProject("jvm")
      .settings(
        libraryDependencies ++= {
          val sbtBV = sbtBinaryVersion.value
          val scalaBV = scalaBinaryVersion.value
          Seq(
            sbtPluginExtra(alphardSbtJvm, sbtBV, scalaBV)
          )
        }
      )
      .dependsOn(core)

  val web =
    noplaySbtProject("web")
      .settings(
        libraryDependencies ++= {
          val sbtBV = sbtBinaryVersion.value
          val scalaBV = scalaBinaryVersion.value
          Seq(
            sbtPluginExtra(alphardSbtWeb, sbtBV, scalaBV),
            sbtPluginExtra(sbtRjs, sbtBV, scalaBV),
            sbtPluginExtra(sbtLess, sbtBV, scalaBV),
            sbtPluginExtra(sbtSass, sbtBV, scalaBV)
          )
        }
      )
      .dependsOn(core)

  lazy val root =
    noplayProject("sbt", isRoot = true)
      .aggregate(core, jvm, web)
}
