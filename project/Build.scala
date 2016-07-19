import io.alphard.sbt._
import io.noplay.sbt.SbtCommunitySettings.autoImport._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {

  //////////
  // CORE //
  //////////

  val alphardSbtVersion = "3.34.0"
  val alphardSbtCore = "io.alphard" %% "alphard-sbt-core" % alphardSbtVersion

  /////////
  // JVM //
  /////////

  val alphardSbtJvm = "io.alphard" %% "alphard-sbt-jvm" % alphardSbtVersion

  /////////
  // WEB //
  /////////

  val alphardSbtWeb = "io.alphard" %% "alphard-sbt-web" % alphardSbtVersion

  var rjsVersion = "2.2.0"
  var rjs = "org.webjars" % "rjs" % rjsVersion

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
            rjs,
            sbtPluginExtra(alphardSbtWeb, sbtBV, scalaBV),
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
