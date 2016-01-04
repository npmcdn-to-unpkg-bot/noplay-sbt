lazy val root = (project in file(".")).enablePlugins(SbtWebStack).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index"
)