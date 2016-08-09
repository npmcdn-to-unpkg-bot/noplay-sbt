lazy val root = (project in file(".")).enablePlugins(SbtNunjucks).settings(
  version := "0.1.0-SNAPSHOT",
  requireMainIndexModule in Assets := "index",
  requireMainIndexModule in TestAssets := "index"
  /*requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("q:'lib/q/q'"))
      sys.error("invalid q path setting!")
  }*/
)