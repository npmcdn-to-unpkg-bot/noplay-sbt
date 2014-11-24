lazy val root = (project in file(".")).enablePlugins(SbtRequireJs).settings(
  requireJsConfigurationPaths in Assets ++= Seq(
    "index" -> "index",
    "test" -> "test"
  ),
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets ++= Seq(
    "index" -> "index",
    "test" -> "test"
  ),
  requireJsMainModuleId in TestAssets := "index"
)