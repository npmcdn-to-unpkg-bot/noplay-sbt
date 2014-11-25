lazy val root = (project in file(".")).enablePlugins(SbtWebUtil).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index"
)