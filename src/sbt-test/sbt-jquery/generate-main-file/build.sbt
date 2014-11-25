lazy val root = (project in file(".")).enablePlugins(SbtJquery).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireJsSettings") := {
    val mainFile = (requireJsMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("jquery:'lib/jquery/jquery'"))
      sys.error("invalid jquery path setting!")
    if (!mainFileContent.contains("jquery:{exports:'jQuery'}"))
      sys.error("invalid jquery shim setting!")
  }
)