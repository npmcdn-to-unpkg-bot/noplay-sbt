lazy val root = (project in file(".")).enablePlugins(SbtJquery).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("jquery:'lib/jquery/jquery'"))
      sys.error("invalid jquery path setting!")
    if (!mainFileContent.contains("jquery:{exports:'jQuery'}"))
      sys.error("invalid jquery shim setting!")
  }
)