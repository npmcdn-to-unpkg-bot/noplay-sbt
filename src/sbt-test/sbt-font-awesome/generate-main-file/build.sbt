lazy val root = (project in file(".")).enablePlugins(SbtFontAwesome).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("'font-awesome':'lib/font-awesome'"))
      sys.error("invalid font awesome path setting!")
  }
)