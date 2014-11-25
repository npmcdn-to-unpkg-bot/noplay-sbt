lazy val root = (project in file(".")).enablePlugins(SbtBootstrapJs).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireJsSettings") := {
    val mainFile = (requireJsMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("bootstrap:'lib/bootstrap'"))
      sys.error("invalid bootstrap js path setting!")
    /*if (!mainFileContent.contains("angular:{exports:'angular'}"))
      sys.error("invalid bootstrap js shim setting!")*/
  }
)