lazy val root = (project in file(".")).enablePlugins(SbtBootstrap).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("bootstrap:'lib/bootstrap'"))
      sys.error("invalid bootstrap js path setting!")
    /*if (!mainFileContent.contains("angular:{exports:'angular'}"))
      sys.error("invalid bootstrap js shim setting!")*/
  }
)