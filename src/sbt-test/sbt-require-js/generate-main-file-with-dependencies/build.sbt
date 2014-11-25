lazy val root = (project in file(".")).enablePlugins(SbtRequireJs).settings(
  requireJsConfigurationPaths in Assets ++= Seq(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireJsConfigurationDeps in Assets += "configuration",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets ++= Seq(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireJsConfigurationDeps in TestAssets += "configuration",
  requireJsMainModuleId in TestAssets := "index"
)