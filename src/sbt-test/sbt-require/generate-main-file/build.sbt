lazy val root = (project in file(".")).enablePlugins(SbtRequire).settings(
  requireConfigurationPaths in Assets ++= Seq(
    "index" -> "index",
    "test" -> "test"
  ),
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets ++= Seq(
    "index" -> "index",
    "test" -> "test"
  ),
  requireMainModuleId in TestAssets := "index"
)