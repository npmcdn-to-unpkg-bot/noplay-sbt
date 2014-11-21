lazy val root = (project in file(".")).enablePlugins(SbtRequireJs, SbtWebServer, SbtWebBrowser).settings(
  requireJsConfigurationPaths in Assets := Map(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireJsConfigurationDeps in Assets += "configuration",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets := Map(
    "configuration" -> "configuration",
    "index" -> "index"
  ),
  requireJsConfigurationDeps in TestAssets += "configuration",
  requireJsMainModuleId in TestAssets := "index"
)