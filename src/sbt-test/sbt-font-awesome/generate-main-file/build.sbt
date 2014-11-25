lazy val root = (project in file(".")).enablePlugins(SbtFontAwesome).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireJsSettings") := {
    val mainFile = (requireJsMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("'font-awesome':'lib/font-awesome'"))
      sys.error("invalid font awesome path setting!")
  }
)