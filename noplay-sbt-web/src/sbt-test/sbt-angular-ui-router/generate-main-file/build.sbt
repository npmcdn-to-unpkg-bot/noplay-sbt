lazy val root = (project in file(".")).enablePlugins(SbtAngularUiRouter).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("'angular-ui-router':'lib/angular-ui-router/angular-ui-router'"))
      sys.error("invalid angular ui router path setting!")
    if (!mainFileContent.contains("'angular-ui-router':{deps:['angular'],init:function(angular){returnangular.module(\"ui.router.compat\");}}"))
      sys.error("invalid angular ui router shim setting!")
  }
)