lazy val root = (project in file(".")).enablePlugins(SbtAngularUiRouter).settings(
  requireJsConfigurationPaths in Assets += "index" -> "index",
  requireJsMainModuleId in Assets := "index",
  requireJsConfigurationPaths in TestAssets += "index" -> "index",
  requireJsMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireJsSettings") := {
    val mainFile = (requireJsMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("'angular-ui-router':'lib/angular-ui-router/angular-ui-router'"))
      sys.error("invalid angular ui router path setting!")
    if (!mainFileContent.contains("'angular-ui-router':{deps:['angular'],init:function(angular){returnangular.module(\"ui.router.compat\");}}"))
      sys.error("invalid angular ui router shim setting!")
  }
)