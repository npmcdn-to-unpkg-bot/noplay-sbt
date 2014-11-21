lazy val root = (project in file(".")).enablePlugins(SbtRequireJs, SbtWebServer, SbtWebBrowser).settings(
  requireJsConfigurationPaths in Assets := Map(
    "index" -> "index",
    "test" -> "test"
  ),
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets := Map(
    "index" -> "index",
    "test" -> "test"
  ),
  requireJsMainModuleId in TestAssets := "index"
)