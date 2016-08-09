lazy val root = (project in file(".")).enablePlugins(SbtAngular).settings(
  requireMainConfigPaths in Assets += "index" -> "index",
  requireCallbackModule in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireCallbackModule in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("angular:'lib/angularjs/angular'"))
      sys.error("invalid angular js path setting!")
    if (!mainFileContent.contains("angular:{exports:'angular'}"))
      sys.error("invalid angular js shim setting!")
  }
)