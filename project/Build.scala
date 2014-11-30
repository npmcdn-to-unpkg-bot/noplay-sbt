import com.byteground.sbt.SbtByTeGround.autoImport._
import com.byteground.sbt._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {
  val sbtWebCoreVersion = "3.12.0"
  val sbtWebCore = "com.byteground" %% "byteground-sbt-web-core-plugins" % sbtWebCoreVersion

  val sbtLessVersion = "1.0.4"
  val sbtLess = "com.typesafe.sbt" %% "sbt-less" % sbtLessVersion

  val sbtRjsVersion = "1.0.7"
  val sbtRjs = "com.typesafe.sbt" %% "sbt-rjs" % sbtRjsVersion
}

object Build
  extends Build {

  import BuildDependencies._

  lazy val root = bytegroundProject("sbt-web-stack-plugins").settings(
      sbtPlugin := true,
      libraryDependencies ++= Seq(
      ),
      libraryDependencies ++= {
        val sbtBV = sbtBinaryVersion.value
        val scalaBV = scalaBinaryVersion.value
        Seq(
          sbtPluginExtra(sbtWebCore, sbtBV, scalaBV),
          sbtPluginExtra(sbtLess, sbtBV, scalaBV),
          sbtPluginExtra(sbtRjs, sbtBV, scalaBV)
        )
      }
  ).enablePlugins(SbtScripted)
}
