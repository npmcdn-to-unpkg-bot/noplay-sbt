lazy val root = (project in file(".")).enablePlugins(SbtAngularJs).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireJsSettings") := {
    val mainFile = (requireJsMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("angular:'lib/angularjs/angular'"))
      sys.error("invalid angular js path setting!")
    if (!mainFileContent.contains("angular:{exports:'angular'}"))
      sys.error("invalid angular js shim setting!")
  }
)