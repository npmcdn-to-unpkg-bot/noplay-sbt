/**
 * Copyright © 2009-2014 ByTeGround, Inc
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.byteground.sbt

import com.byteground.sbt.SbtWebIndex.autoImport._
import com.byteground.sbt.util.Javascript
import com.typesafe.sbt.rjs.SbtRjs
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import sbt.Keys._
import sbt._

object SbtRequireJs
  extends AutoPlugin {
  override lazy val requires = SbtWebIndex && SbtRjs

  object autoImport {

    object RequireJsModule {
      type Id = String
      type Path = String
    }

    case class RequireJsPackage(name: String, main: String) {
      private[SbtRequireJs] lazy val toMap: Map[String, Any] =
        Map(
          "name" -> name,
          "main" -> main
        )
    }

    object RequireJsConfiguration {
      type Paths = Seq[(RequireJsModule.Id, RequireJsModule.Path)]
      type Bundles = Seq[(RequireJsModule.Id, Seq[RequireJsModule.Path])]
      type Shim = Seq[(String, Shim.Config)]
      object Shim {
        case class Config(deps: Seq[RequireJsModule.Id] = Nil,
                          exports: Option[String] = None,
                          init: Option[Javascript.Function] = None) {
          private[SbtRequireJs] lazy val toMap: Map[String, Any] =
            Map(
              "deps" -> deps,
              "exports" -> exports,
              "init" -> init
            )
        }

      }
      type _Map = Seq[(String, Map[RequireJsModule.Id, RequireJsModule.Id])]
      type Packages = Seq[(RequireJsModule.Id, RequireJsPackage)]
      type Config = Seq[(RequireJsModule.Id, Map[String, Any])]
    }

    case class RequireJsConfiguration(baseUrl: Option[String],
                                      paths: RequireJsConfiguration.Paths,
                                      bundles: RequireJsConfiguration.Bundles,
                                      shim: RequireJsConfiguration.Shim,
                                      map: RequireJsConfiguration._Map,
                                      config: RequireJsConfiguration.Config,
                                      packages: RequireJsConfiguration.Packages,
                                      nodeIdCompat: Boolean,
                                      waitSeconds: Int,
                                      context: Option[String],
                                      deps: Seq[RequireJsModule.Id],
                                      callback: Option[Javascript.Function],
                                      enforceDefine: Boolean,
                                      xhtml: Boolean,
                                      urlArgs: Option[String],
                                      scriptType: String,
                                      skipDataMain: Boolean) {
      private[SbtRequireJs] def toMap(implicit logger: Logger): Map[String, _] =
        Map(
          "baseUrl" -> baseUrl,
          asMap("paths", paths),
          asMap("bundles", bundles),
          asMap("shim", shim.map { case (k, v) => (k, v.toMap)}),
          asMap("map", map.groupBy(_._1).toSeq.map { case (k, v) => (k, v.unzip._2.reduce(_ ++ _)) }),
          asMap("config", config),
          asMap("packages", packages.map { case (k, v) => (k, v.toMap)}),
          "nodeIdCompat" -> nodeIdCompat,
          "waitSeconds" -> waitSeconds,
          "context" -> context,
          "deps" -> deps,
          "callback" -> callback,
          "enforceDefine" -> enforceDefine,
          "xhtml" -> xhtml,
          "urlArgs" -> urlArgs,
          "scriptType" -> scriptType,
          "skipDataMain" -> skipDataMain
        )

      // (Map.empty[RequireJsModule.Id, RequireJsModule.Id] /: v.unzip._2)(_ ++ _)
      private[SbtRequireJs] def asMap[K, V](key: String, seq: Seq[(K, V)])(implicit logger: Logger): (String, Map[K, V]) = {
        seq.groupBy(_._1) foreach {
          case (groupKey, groupValues) =>
            if (groupValues.size > 1)
              logger.warn(s"duplicate key '$groupKey' in '$key!")
        }
        (key, Map(seq: _*))
      }
    }

    val requireJsVersion = settingKey[String]("The web jars require js version")
    val requireJsPath = settingKey[String]("The web jars require js path")
    val requireJsConfigurationBaseUrl = settingKey[Option[String]]("The root path to use for all module lookup")
    val requireJsConfigurationPaths = settingKey[RequireJsConfiguration.Paths](
      "The path mappings for module names not found directly under baseUrl"
    )
    val requireJsConfigurationBundles = settingKey[RequireJsConfiguration.Bundles](
      "The bundles allow configuring multiple module IDs to be found in another script"
    )
    val requireJsConfigurationShim = settingKey[RequireJsConfiguration.Shim](
      """Configure the dependencies, exports, and custom initialization for older,
        |traditional "browser globals" scripts that do not use define()
        |to declare the dependencies and set a module value.
      """.stripMargin
    )
    val requireJsConfigurationMap = settingKey[RequireJsConfiguration._Map](
      """For the given module prefix, instead of loading the module
        |with the given ID, substitute a different module ID
        | """.stripMargin
    )
    val requireJsConfigurationConfig = settingKey[RequireJsConfiguration.Config](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireJsConfigurationPackages = settingKey[RequireJsConfiguration.Packages](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireJsConfigurationNodeIdCompat = settingKey[Boolean](
      """Node treats module ID example.js and example the same. By default these are two different IDs in RequireJS.
        |If you end up using modules installed from npm, then you may need
        |to set this config value to true to avoid resolution issues.
      """.stripMargin
    )
    val requireJsConfigurationWaitSeconds = settingKey[Int](
      """The number of seconds to wait before giving up on loading a script. Setting it to 0 disables the timeout.
        |The default is 7 seconds.
      """.stripMargin
    )
    val requireJsConfigurationContext = settingKey[Option[String]](
      """A name to give to a loading context. This allows require.js to load multiple versions of modules in a page,
        |as long as each top-level require call specifies a unique context string. To use it correctly,
        |see the Multiversion Support section.
      """.stripMargin
    )
    val requireJsConfigurationDeps = settingKey[Seq[RequireJsModule.Id]](
      """An array of dependencies to load.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify dependencies to load as soon as require() is defined.
      """.stripMargin)
    val requireJsConfigurationCallback = settingKey[Option[Javascript.Function]](
      """A function to execute after deps have been loaded.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify a function to require after the configuration's deps array has been loaded.
      """.stripMargin)
    val requireJsConfigurationEnforceDefine = settingKey[Boolean](
      """If set to true, an error will be thrown if a script loads that does not call define()
        |or have a shim exports string value that can be checked.""".stripMargin
    )
    val requireJsConfigurationXhtml = settingKey[Boolean](
      """If set to true, document.createElementNS() will be used to create script elements.
      """.stripMargin
    )
    val requireJsConfigurationUrlArgs = settingKey[Option[String]](
      "Extra query string arguments appended to URLs that RequireJS uses to fetch resources."
    )
    val requireJsConfigurationScriptType = settingKey[String](
      """Specify the value for the type="" attribute used for script tags inserted into the document by RequireJS.
        |Default is "text/javascript.
      """.stripMargin
    )
    val requireJsConfigurationSkipDataMain = settingKey[Boolean](
      """If set to true, skips the data-main attribute scanning done to start module loading.
        | Useful if RequireJS is embedded in a utility library that may interact with other RequireJS library on the page,
        | and the embedded version should not do data-main loading.
      """.stripMargin
    )
    val requireJsConfiguration = settingKey[RequireJsConfiguration]("The full configuration object")
    val requireJsDirectory = settingKey[File]("The main file directory")
    val requireJsMainName = settingKey[String]("The main file name")
    val requireJsMainModuleId = settingKey[RequireJsModule.Id]("The main module id")
    val requireJsMainTemplateFile = settingKey[Option[File]]("The main template file")
    val requireJsMainFile = settingKey[File]("The main file")
    val requireJsMainGenerator = taskKey[Seq[File]]("Generate the config file")
    val requireJsIncludeFilter = settingKey[FileFilter]("The include filter generated from paths")
    val requireJsExcludeFilter = settingKey[FileFilter]("The exclude filter generated from paths")

    lazy val unscopedProjectSettings = Seq(
      requireJsConfigurationBaseUrl := None,
      requireJsConfigurationPaths := Nil,
      requireJsConfigurationBundles := Nil,
      requireJsConfigurationShim := Nil,
      requireJsConfigurationMap := Nil,
      requireJsConfigurationConfig := Nil,
      requireJsConfigurationPackages := Nil,
      requireJsConfigurationNodeIdCompat := false,
      requireJsConfigurationWaitSeconds := 7,
      requireJsConfigurationContext := None,
      requireJsConfigurationDeps := Nil,
      requireJsConfigurationCallback := Some(Javascript.Function(
        s"""function() { require(['${requireJsMainModuleId.value}']); }""")
      ),
      requireJsConfigurationEnforceDefine := false,
      requireJsConfigurationXhtml := false,
      requireJsConfigurationUrlArgs := None,
      requireJsConfigurationScriptType := "text/javascript",
      requireJsConfigurationSkipDataMain := false,
      requireJsConfiguration := RequireJsConfiguration(
        requireJsConfigurationBaseUrl.value,
        requireJsConfigurationPaths.value,
        requireJsConfigurationBundles.value,
        requireJsConfigurationShim.value,
        requireJsConfigurationMap.value,
        requireJsConfigurationConfig.value,
        requireJsConfigurationPackages.value,
        requireJsConfigurationNodeIdCompat.value,
        requireJsConfigurationWaitSeconds.value,
        requireJsConfigurationContext.value,
        requireJsConfigurationDeps.value,
        requireJsConfigurationCallback.value,
        requireJsConfigurationEnforceDefine.value,
        requireJsConfigurationXhtml.value,
        requireJsConfigurationUrlArgs.value,
        requireJsConfigurationScriptType.value,
        requireJsConfigurationSkipDataMain.value
      ),
      requireJsDirectory := sourceManaged.value / "require-js",
      requireJsMainName := "main.js",
      requireJsMainTemplateFile := None,
      requireJsMainFile := requireJsDirectory.value / requireJsMainName.value,
      requireJsMainGenerator := {
        implicit val logger = streams.value.log
        val configuration = Javascript.toJs(requireJsConfiguration.value.toMap)
        val moduleId = requireJsMainModuleId.value
        val mainTemplate = requireJsMainTemplateFile.value.map(IO.read(_)) getOrElse {
          IO.readStream(getClass.getResource(DefaultRequireJsMainTemplate).openStream())
        }
        val mainFile = requireJsMainFile.value
        if (!mainFile.exists()) {
          mainFile.getParentFile.mkdirs()
          mainFile.createNewFile()
        }
        IO.write(
          mainFile,
          util.FreeMarker.render(
            mainTemplate,
            Map(
              "configuration" -> configuration,
              "moduleId" -> moduleId
            )
          )
        )
        Seq(mainFile)
      },
      requireJsIncludeFilter := {
        val paths = requireJsConfigurationPaths.value.map(_._2)
        new SimpleFileFilter({
          case file if file.isFile =>
            val path = file.getAbsolutePath.replace('\\', '/')
            (false /: paths)(_ || path.contains(_))
          case _ =>
            false
        })
      },
      requireJsExcludeFilter := NothingFilter,
      includeFilter := includeFilter.value || requireJsIncludeFilter.value,
      excludeFilter := excludeFilter.value || requireJsExcludeFilter.value,
      managedSourceDirectories <+= requireJsDirectory,
      sourceGenerators <+= requireJsMainGenerator,
      webIndexScripts ++= Seq[Script](
        SbtWebIndex.autoImport.Script(
          requireJsPath.value,
          async = true,
          attributes = Map("data-main" -> requireJsMainName.value)
        )
      )
    )
  }

  import com.byteground.sbt.SbtRequireJs.autoImport._

  override lazy val projectSettings =
    inConfig(Assets)(unscopedProjectSettings) ++
      inConfig(TestAssets)(unscopedProjectSettings) ++ Seq(
      requireJsVersion := "2.1.14-3",
      requireJsPath := webModulesLib.value + "/requirejs/require.js",
      libraryDependencies += "org.webjars" % "requirejs" % requireJsVersion.value
    )

  private val DefaultRequireJsMainTemplate = "/com/byteground/sbt/requirejs.js.ftl"
}