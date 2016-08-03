/**
 * Copyright © 2009-2016 Hydra Technologies, Inc
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
package io.noplay.sbt.web.require

import java.io.File

import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import io.alphard.sbt.util.Javascript
import io.noplay.sbt.web.SbtWebIndex
import SbtWebIndex.autoImport._
import io.noplay.sbt.web.require.SbtRjs.autoImport._
import io.noplay.sbt.web.SbtWebIndex
import sbt.Keys._
import sbt._

object SbtRequire
  extends AutoPlugin {

  private val DefaultRequireMainTemplate = "/io/noplay/sbt/web/require/requirejs.js.ftl"

  override val requires = SbtWebIndex && SbtRjs

  object autoImport {

    object RequireModule {
      type Id = String
      type Path = String
      object Path {
        def minify(path: String, minified: Boolean = false) = path + (if (minified) ".min" else "")
        def filename(path: String, extension: String = "js"): String = path + "." + extension
        def relativize(path: String): String = if (path.startsWith("/")) path.substring(1) else path
      }
    }

    case class RequirePackage(name: String, main: String) {
      private[SbtRequire] lazy val toMap: Map[String, Any] =
        Map(
          "name" -> name,
          "main" -> main
        )
    }

    object RequireConfiguration {
      type Paths = Seq[(RequireModule.Id, RequireModule.Path)]
      type Bundles = Seq[(RequireModule.Id, Seq[RequireModule.Path])]
      type Shim = Seq[(String, Shim.Config)]

      object Shim {

        case class Config(deps: Seq[RequireModule.Id] = Nil,
                          exports: Option[String] = None,
                          init: Option[Javascript.Function] = None) {
          private[SbtRequire] lazy val toMap: Map[String, Any] =
            Map(
              "deps" -> deps,
              "exports" -> exports,
              "init" -> init
            )
        }

      }

      type _Map = Seq[(String, Map[RequireModule.Id, RequireModule.Id])]
      type Packages = Seq[(RequireModule.Id, RequirePackage)]
      type Config = Seq[(RequireModule.Id, Map[String, Any])]
    }

    case class RequireConfiguration(baseUrl: Option[String],
                                    paths: RequireConfiguration.Paths,
                                    bundles: RequireConfiguration.Bundles,
                                    shim: RequireConfiguration.Shim,
                                    map: RequireConfiguration._Map,
                                    config: RequireConfiguration.Config,
                                    packages: RequireConfiguration.Packages,
                                    nodeIdCompat: Boolean,
                                    waitSeconds: Int,
                                    context: Option[String],
                                    deps: Seq[RequireModule.Id],
                                    callback: Option[Javascript.Function],
                                    enforceDefine: Boolean,
                                    xhtml: Boolean,
                                    urlArgs: Option[String],
                                    scriptType: String,
                                    skipDataMain: Boolean) {
      private[SbtRequire] def toMap(implicit logger: Logger): Map[String, _] =
        Map(
          "baseUrl" -> baseUrl,
          asMap("paths", paths),
          asMap("bundles", bundles),
          asMap("shim", shim.map { case (k, v) => (k, v.toMap) }),
          asMap("map", map.groupBy(_._1).toSeq.map { case (k, v) => (k, v.unzip._2.reduce(_ ++ _)) }),
          asMap("config", config),
          asMap("packages", packages.map { case (k, v) => (k, v.toMap) }),
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

      // (Map.empty[RequireModule.Id, RequireModule.Id] /: v.unzip._2)(_ ++ _)
      private[SbtRequire] def asMap[K, V](key: String, seq: Seq[(K, V)])(implicit logger: Logger): (String, Map[K, V]) = {
        seq.groupBy(_._1) foreach {
          case (groupKey, groupValues) =>
            if (groupValues.size > 1)
              logger.warn(s"duplicate key '$groupKey' in '$key!")
        }
        (key, Map(seq: _*))
      }
    }

    val requireConfigurationBaseUrl = settingKey[Option[String]]("The root path to use for all module lookup")
    val requireConfigurationPaths = settingKey[RequireConfiguration.Paths](
      "The path mappings for module names not found directly under baseUrl"
    )
    val requireConfigurationBundles = settingKey[RequireConfiguration.Bundles](
      "The bundles allow configuring multiple module IDs to be found in another script"
    )
    val requireConfigurationShim = settingKey[RequireConfiguration.Shim](
      """Configure the dependencies, exports, and custom initialization for older,
        |traditional "browser globals" scripts that do not use define()
        |to declare the dependencies and set a module value.
      """.stripMargin
    )
    val requireConfigurationMap = settingKey[RequireConfiguration._Map](
      """For the given module prefix, instead of loading the module
        |with the given ID, substitute a different module ID
      """.stripMargin
    )
    val requireConfigurationConfig = settingKey[RequireConfiguration.Config](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireConfigurationPackages = settingKey[RequireConfiguration.Packages](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireConfigurationNodeIdCompat = settingKey[Boolean](
      """Node treats module ID example.js and example the same. By default these are two different IDs in RequireJS.
        |If you end up using modules installed from npm, then you may need
        |to set this config value to true to avoid resolution issues.
      """.stripMargin
    )
    val requireConfigurationWaitSeconds = settingKey[Int](
      """The number of seconds to wait before giving up on loading a script. Setting it to 0 disables the timeout.
        |The default is 7 seconds.
      """.stripMargin
    )
    val requireConfigurationContext = settingKey[Option[String]](
      """A name to give to a loading context. This allows require.js to load multiple versions of modules in a page,
        |as long as each top-level require call specifies a unique context string. To use it correctly,
        |see the Multiversion Support section.
      """.stripMargin
    )
    val requireConfigurationDeps = settingKey[Seq[RequireModule.Id]](
      """An array of dependencies to load.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify dependencies to load as soon as require() is defined.
      """.stripMargin)
    val requireConfigurationCallback = settingKey[Option[Javascript.Function]](
      """A function to execute after deps have been loaded.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify a function to require after the configuration's deps array has been loaded.
      """.stripMargin)
    val requireConfigurationEnforceDefine = settingKey[Boolean](
      """If set to true, an error will be thrown if a script loads that does not call define()
        |or have a shim exports string value that can be checked.""".stripMargin
    )
    val requireConfigurationXhtml = settingKey[Boolean](
      """If set to true, document.createElementNS() will be used to create script elements.
      """.stripMargin
    )
    val requireConfigurationUrlArgs = settingKey[Option[String]](
      "Extra query string arguments appended to URLs that RequireJS uses to fetch resources."
    )
    val requireConfigurationScriptType = settingKey[String](
      """Specify the value for the type="" attribute used for script tags inserted into the document by RequireJS.
        |Default is "text/javascript.
      """.stripMargin
    )
    val requireConfigurationSkipDataMain = settingKey[Boolean](
      """If set to true, skips the data-main attribute scanning done to start module loading.
        | Useful if RequireJS is embedded in a utility library that may interact with other RequireJS library on the page,
        | and the embedded version should not do data-main loading.
      """.stripMargin
    )
    val requireConfiguration = settingKey[RequireConfiguration]("The full configuration object")
    val requireVersion = settingKey[String]("The require js version")
    val requireMinified = settingKey[Boolean]("If true the minified versions of modules in paths are used")
    val requireCDN = settingKey[Boolean]("If true the CDN versions of modules in paths are used")
    val requireOptimized = settingKey[Boolean]("If true an r.js optimization state is added to the pipeline")
    val requirePath = settingKey[String]("The web jars require js path")
    val requireDirectory = settingKey[File]("The main file directory")
    val requireMainModule = settingKey[String]("The main module name")
    val requireMainPath = settingKey[String]("The main file path")
    val requireMainTemplateFile = settingKey[Option[File]]("The main template file")
    val requireMainFile = settingKey[File]("The main file")
    val requireMainGenerator = taskKey[Seq[File]]("Generate the config file")
    val requireIncludeFilter = settingKey[FileFilter]("The include filter generated from paths")
    val requireExcludeFilter = settingKey[FileFilter]("The exclude filter generated from paths")
    val requireCallbackModule = settingKey[RequireModule.Id]("The callback module name")

    lazy val unscopedProjectSettings = Seq(
      requireConfigurationBaseUrl := None,
      requireConfigurationPaths := Seq(
        requireMainModule.value -> RequireModule.Path.minify(requireMainModule.value, requireMinified.value)
      ),
      requireConfigurationBundles := Nil,
      requireConfigurationShim := Nil,
      requireConfigurationMap := Nil,
      requireConfigurationConfig := Nil,
      requireConfigurationPackages := Nil,
      requireConfigurationNodeIdCompat := false,
      requireConfigurationWaitSeconds := 7,
      requireConfigurationContext := None,
      requireConfigurationDeps := Nil,
      requireConfigurationCallback := Some(Javascript.Function(
        s"""function() { require(['${requireCallbackModule.value}']); }""")
      ),
      requireConfigurationEnforceDefine := false,
      requireConfigurationXhtml := false,
      requireConfigurationUrlArgs := None,
      requireConfigurationScriptType := "text/javascript",
      requireConfigurationSkipDataMain := false,
      requireConfiguration := RequireConfiguration(
        requireConfigurationBaseUrl.value,
        requireConfigurationPaths.value,
        requireConfigurationBundles.value,
        requireConfigurationShim.value,
        requireConfigurationMap.value,
        requireConfigurationConfig.value,
        requireConfigurationPackages.value,
        requireConfigurationNodeIdCompat.value,
        requireConfigurationWaitSeconds.value,
        requireConfigurationContext.value,
        requireConfigurationDeps.value,
        requireConfigurationCallback.value,
        requireConfigurationEnforceDefine.value,
        requireConfigurationXhtml.value,
        requireConfigurationUrlArgs.value,
        requireConfigurationScriptType.value,
        requireConfigurationSkipDataMain.value
      ),
      requireMinified := true,
      requireCDN := true,
      requireOptimized := true,
      requireDirectory := sourceManaged.value / "require-js",
      requirePath := {
        val path = if (requireCDN.value)
          s"//cdn.jsdelivr.net/webjars/requirejs/${requireVersion.value}/require"
        else
          s"/${webModulesLib.value}/requirejs/require"
        RequireModule.Path.filename(RequireModule.Path.minify(path, requireMinified.value))
      },
      requireMainModule := "main",
      requireMainPath := RequireModule.Path.minify(requireConfigurationBaseUrl.value.getOrElse("") + "/" + requireMainModule.value, requireMinified.value),
      requireMainTemplateFile := None,
      requireMainFile := requireDirectory.value / RequireModule.Path.filename(RequireModule.Path.relativize(requireMainPath.value)),
      requireMainGenerator := {
        implicit val logger = streams.value.log
        val configuration = Javascript.toJs(requireConfiguration.value.toMap)
        val moduleId = requireCallbackModule.value
        val mainTemplate = requireMainTemplateFile.value.map(IO.read(_)) getOrElse {
          IO.readStream(getClass.getResource(DefaultRequireMainTemplate).openStream())
        }
        val mainFile = requireMainFile.value
        if (!mainFile.exists()) {
          mainFile.getParentFile.mkdirs()
          mainFile.createNewFile()
        }
        IO.write(
          mainFile,
          io.alphard.sbt.util.FreeMarker.render(
            mainTemplate,
            Map(
              "configuration" -> configuration,
              "moduleId" -> moduleId
            )
          )
        )
        Seq(mainFile)
      },
      requireIncludeFilter := {
        val paths = requireConfigurationPaths.value.map(_._2)
        new SimpleFileFilter({
          case file if file.isFile =>
            val path = file.getAbsolutePath.replace('\\', '/')
            (false /: paths) (_ || path.contains(_))
          case _ =>
            false
        })
      },
      requireExcludeFilter := NothingFilter,
      includeFilter := includeFilter.value || requireIncludeFilter.value,
      excludeFilter := excludeFilter.value || requireExcludeFilter.value,
      managedSourceDirectories <+= requireDirectory,
      sourceGenerators <+= requireMainGenerator,
      webIndexScripts ++= Seq[Script](
        SbtWebIndex.autoImport.Script(
          requirePath.value,
          async = true,
          attributes = Map("data-main" -> RequireModule.Path.filename(requireMainPath.value))
        )
      )
    )
  }

  import autoImport._

  val rjsSettings = Seq(
    RjsKeys.version := requireVersion.value,
    RjsKeys.baseUrl := (requireConfigurationBaseUrl in Assets).value.map(RequireModule.Path.relativize).getOrElse("."),
    RjsKeys.mainConfig := RequireModule.Path.minify((requireMainModule in Assets).value, (requireMinified in Assets).value),
    RjsKeys.mainConfigFile := new File(RequireModule.Path.relativize(s"${RjsKeys.baseUrl.value}/${RequireModule.Path.filename(RjsKeys.mainConfig.value)}")),
    pipelineStages ++= (if ((requireOptimized in Assets).value) Seq(rjs) else Nil)
  )

  import SbtRequire.autoImport._

  override val projectSettings =
    inConfig(Assets)(unscopedProjectSettings) ++
      inConfig(TestAssets)(unscopedProjectSettings) ++ Seq(
      requireVersion := "2.2.0",
      libraryDependencies ++= Seq(
        "org.webjars" % "requirejs" % requireVersion.value
      )
    ) ++ rjsSettings
}