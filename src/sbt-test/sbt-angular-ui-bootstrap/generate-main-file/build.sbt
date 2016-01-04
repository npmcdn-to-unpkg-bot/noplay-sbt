lazy val root = (project in file(".")).enablePlugins(SbtAngularUiBootstrap).settings(
  requireConfigurationPaths in Assets += "index" -> "index",
  requireMainModuleId in Assets := "index",
  requireConfigurationPaths in TestAssets += "index" -> "index",
  requireMainModuleId in TestAssets := "index",
  TaskKey[Unit]("checkRequireSettings") := {
    val mainFile = (requireMainFile in Assets).value
    val mainFileContent = IO.read(mainFile).replace('\n', ' ').replace('\r', ' ').replace(" ", "")
    if (!mainFileContent.contains("'angular-ui-bootstrap':'lib/angular-ui-bootstrap/ui-bootstrap-tpls'"))
      sys.error("invalid angular ui bootstrap path setting!")
    if (!mainFileContent.contains("'angular-ui-bootstrap':{deps:['angular'],init:function(angular){returnangular.module(\"ui.bootstrap\");}}"))
      sys.error("invalid angular ui bootstrap shim setting!")
  }
)