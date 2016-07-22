import io.alphard.sbt._
import io.noplay.sbt.SbtCommunitySettings.autoImport._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {

  val alphardSbtVersion = "3.36.0"
  val alphardSbtCore = "io.alphard" %% "alphard-sbt-core" % alphardSbtVersion
  val alphardSbtJvm = "io.alphard" %% "alphard-sbt-jvm" % alphardSbtVersion
  val alphardSbtWeb = "io.alphard" %% "alphard-sbt-web" % alphardSbtVersion

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
            sbtPluginExtra(alphardSbtWeb, sbtBV, scalaBV)
          )
        }
      )
      .dependsOn(core)

  lazy val root =
    noplayProject("sbt", isRoot = true)
      .aggregate(core, jvm, web)
}
