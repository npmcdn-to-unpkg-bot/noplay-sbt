lazy val root = (project in file(".")).enablePlugins(SbtRequire).settings(
  requireConfigurationPaths in Assets ++= Seq(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireConfigurationDeps in Assets += "configuration",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets ++= Seq(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireConfigurationDeps in TestAssets += "configuration",
  requireMainModuleId in TestAssets := "index"
)