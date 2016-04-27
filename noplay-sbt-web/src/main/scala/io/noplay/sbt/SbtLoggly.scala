package io.noplay.sbt

import io.alphard.sbt.SbtNpm
import io.alphard.sbt.SbtNpm.autoImport._
import io.noplay.sbt.SbtRequire.autoImport.RequireConfiguration
import io.noplay.sbt.SbtRequire.autoImport._
import io.alphard.sbt.util.Javascript
import com.typesafe.sbt.web.Import.WebKeys
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt._

object SbtLoggly
  extends AutoPlugin {
  override val requires = SbtNpm && SbtRequire

  object autoImport {
    val logglyVersion = settingKey[String]("Loggly version")
    val logglySendConsoleErrors = settingKey[Boolean]("Loggly send console errors")
    val logglyTags = settingKey[Seq[String]]("Loggly tags")
    val logglyLibraryName = settingKey[String]("Loggly library name")
    val logglyLibraryFileName = settingKey[String]("Loggly library file name")
    val logglyWebModulesDirectoryName = settingKey[String]("Loggly web modules directory name")
    val logglyWebModulesDirectory = settingKey[File]("Loggly web modules directory")
    val logglyWebModulesGenerator = settingKey[Task[Seq[File]]]("Loggly web modules generator")
  }

  import autoImport._

  val unscopedProjectSettings = Seq(
    logglyWebModulesDirectoryName := "loggly",
    logglyWebModulesDirectory := webModuleDirectory.value / logglyWebModulesDirectoryName.value,
    logglyWebModulesGenerator := Def.task {
      val directory = logglyWebModulesDirectory.value / webModulesLib.value / logglyLibraryName.value
      if (!directory.exists())
        directory.mkdirs()
      val logglyNodeModules = nodeModules.value filter {
        nodeModule =>
          val path = nodeModule.getAbsolutePath
          path.contains(logglyLibraryName.value) &&
            path.contains(logglyLibraryFileName.value)
      } map {
        file =>
          val newFile = directory / file.getName
          if (!newFile.exists())
            newFile.createNewFile()
          IO.copyFile(file, newFile)
          newFile
      }
      logglyNodeModules
    }.dependsOn(WebKeys.nodeModules).taskValue,
    webModuleDirectories <+= logglyWebModulesDirectory,
    webModuleGenerators <+= logglyWebModulesGenerator,
    requireConfigurationPaths += "loggly" -> s"/${webModulesLib.value}/${logglyLibraryName.value}/${logglyLibraryFileName.value}",
    requireConfigurationShim ++= Seq(
      "loggly" -> RequireConfiguration.Shim.Config(
        Seq("module"),
        exports = Some("_LTracker"),
        init = Some(
          Javascript.Function(
            s"""function(module) {
                |  var config = (module.config && module.config()) || {};
                |
                |  if (config.debug)
                |    console.debug('[loggly]', 'config', config);
                |
                |  var key = config.key;
                |  var sendConsoleErrors = config.sendConsoleErrors === undefined || !!config.sendConsoleErrors;
                |  var sendLogErrors = config.sendLogErrors !== undefined && !!config.sendLogErrors;
                |  var sendRequireErrors = config.sendRequireErrors !== undefined && !!config.sendRequireErrors;
                |  var tag = config.tag || 'loggly-jslogger';
                |  var useDomainProxy = config.useDomainProxy !== undefined && !!config.useDomainProxy;
                |  var trackerConfig = {
                |    'logglyKey': key,
                |    'sendConsoleErrors' : sendConsoleErrors,
                |    'tag' : tag,
                |    'useDomainProxy': useDomainProxy
                |  };
                |
                |  if (config.debug)
                |    console.debug('[loggly]', 'tracker config', trackerConfig);
                |
                |  var _LTracker = window._LTracker || [];
                |  _LTracker.push(trackerConfig);
                |
                |  if (sendLogErrors) {
                |    var _consoleError = window.console.error || function() {};
                |    window.console.error = function() {
                |      _LTracker.push({
                |        category: 'ConsoleError',
                |        exception: arguments
                |      });
                |      return _consoleError.apply(this, arguments);
                |    };
                |  }
                |
                |  if (sendRequireErrors) {
                |    var _onError = requirejs.onError;
                |    requirejs.onError = function (error) {
                |      _LTracker.push({
                |        category: 'RequireJsException',
                |        exception: error
                |      });
                |      if (_onError && typeof _onError === 'function') {
                |        _onError.apply(requirejs, arguments);
                |      }
                |      throw error;
                |    };
                |  }
                |
                |  if (config.debug)
                |    console.debug('[loggly]', 'tracker', _LTracker);
                |}
              """.stripMargin
          )
        )
      )
    )
  )

  override val projectSettings = Seq(
    logglyVersion := "2.1.0",
    logglyLibraryName := "loggly-jslogger",
    logglyLibraryFileName := "loggly.tracker",
    npmDependencies ++= Seq(
      logglyLibraryName.value -> logglyVersion.value
    )
  ) ++
    inConfig(Assets)(unscopedProjectSettings) ++
    inConfig(TestAssets)(unscopedProjectSettings)
}