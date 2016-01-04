import com.byteground.sbt.SbtByTeGround.autoImport._
import com.byteground.sbt._
import sbt.Defaults._
import sbt.Keys._
import sbt._

object BuildDependencies {
  val sbtWebCoreVersion = "3.24.0"
  val sbtWebCore = "com.byteground" %% "byteground-sbt-web-core-plugins" % sbtWebCoreVersion

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

  lazy val root = bytegroundProject("sbt-web-stack-plugins", isRoot = true).settings(
    sbtPlugin := true,
    libraryDependencies ++= {
      val sbtBV = sbtBinaryVersion.value
      val scalaBV = scalaBinaryVersion.value
      Seq(
        sbtPluginExtra(sbtWebCore, sbtBV, scalaBV),
        sbtPluginExtra(sbtRjs, sbtBV, scalaBV),
        sbtPluginExtra(sbtLess, sbtBV, scalaBV),
        sbtPluginExtra(sbtSass, sbtBV, scalaBV)
      )
    }
  ).enablePlugins(SbtScripted)
}
