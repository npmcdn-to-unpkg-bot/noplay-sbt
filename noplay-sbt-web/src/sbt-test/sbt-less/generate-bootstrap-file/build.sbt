lazy val root = (project in file(".")).enablePlugins(SbtLess).settings(v
  libraryDependencies ++= Seq(
    "org.webjars" % "bootstrap" % "3.3.6"
  ),
  includeFilter in (Assets, LessKeys.less) := "*.less",
  excludeFilter in (Assets, LessKeys.less) := "_*.less"
)