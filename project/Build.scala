import com.byteground.sbt.SbtByTeGround.autoImport._
import com.byteground.sbt._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {
  val sbtWebCoreVersion = "3.10.0"
  val sbtWebCore = "com.byteground" %% "byteground-sbt-web-core-plugins" % sbtWebCoreVersion

  val sbtRjsVersion = "1.0.6"
  val sbtRjs = "com.typesafe.sbt" %% "sbt-rjs" % sbtRjsVersion
}

object Build
  extends Build {

  import BuildDependencies._

  lazy val root = bytegroundProject("sbt-web-requirejs-plugins").settings(
      sbtPlugin := true,
      libraryDependencies ++= Seq(
      ),
      libraryDependencies ++= {
        val sbtBV = sbtBinaryVersion.value
        val scalaBV = scalaBinaryVersion.value
        Seq(
          sbtPluginExtra(sbtWebCore, sbtBV, scalaBV),
          sbtPluginExtra(sbtRjs, sbtBV, scalaBV)
        )
      }
  ).enablePlugins(SbtScripted)
}
