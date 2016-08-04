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

import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.js.{JavaScript, JS}
import io.noplay.sbt.web.SbtWebIndex
import io.noplay.sbt.web.SbtWebIndex.autoImport._
import io.noplay.sbt.web.require.SbtRjs.autoImport._
import sbt.Keys._
import sbt._

object SbtRequire
  extends AutoPlugin {

  private val DefaultRequireMainTemplate = "/io/noplay/sbt/web/require/requirejs.js.ftl"

  override val requires = SbtWebIndex && SbtJsTask && SbtRjs

  object autoImport {

    type RequireId = String
    type RequirePath = String

    object RequirePath {
      def minify(path: String, minified: Boolean = false) = path + (if (minified) ".min" else "")
      def filename(path: String, extension: String = "js"): String = path + "." + extension
      def relativize(path: String): String = if (path.startsWith("/")) path.substring(1) else path
    }

    type RequirePaths = Seq[(RequireId, RequirePath)]
    type RequireBundles = Seq[(RequireId, Seq[RequirePath])]
    type RequireShim = Seq[(String, RequireShimConfig)]
    final case class RequireShimConfig(deps: Seq[RequireId] = Nil, exports: Option[String] = None, init: Option[JavaScript] = None)
    type RequireMap = Seq[(String, Map[RequireId, RequireId])]
    type RequireConfig = Seq[(RequireId, JS[_])]
    final case class RequirePackage(name: String, main: String)
    type RequirePackages = Seq[(RequireId, RequirePackage)]

    sealed trait RequireOptimizer
    final case class Uglify(configuration: JS.Object) extends RequireOptimizer
    final case class Uglify2(configuration: JS.Object) extends RequireOptimizer
    final case class Closure(configuration: JS.Object) extends RequireOptimizer

    ///////////////////
    // CONFIGURATION //
    ///////////////////

    val requireConfigurationBaseUrl = settingKey[Option[String]]("The root path to use for all module lookup")
    val requireConfigurationPaths = settingKey[RequirePaths](
      "The path mappings for module names not found directly under baseUrl"
    )
    val requireConfigurationBundles = settingKey[RequireBundles](
      "The bundles allow configuring multiple module IDs to be found in another script"
    )
    val requireConfigurationShim = settingKey[RequireShim](
      """Configure the dependencies, exports, and custom initialization for older,
        |traditional "browser globals" scripts that do not use define()
        |to declare the dependencies and set a module value.
      """.stripMargin
    )
    val requireConfigurationMap = settingKey[RequireMap](
      """For the given module prefix, instead of loading the module
        |with the given ID, substitute a different module ID
      """.stripMargin
    )
    val requireConfigurationConfig = settingKey[RequireConfig](
      """There is a common need to pass configuration info to a module.
        |That configuration info is usually known as part of the application,
        |and there needs to be a way to pass that down to a module.
      """.stripMargin)
    val requireConfigurationPackages = settingKey[RequirePackages](
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
    val requireConfigurationDeps = settingKey[Seq[RequireId]](
      """An array of dependencies to load.
        |Useful when require is defined as a config object before require.js is loaded,
        |and you want to specify dependencies to load as soon as require() is defined.
      """.stripMargin)
    val requireConfigurationCallback = settingKey[Option[JavaScript]](
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
    val requireConfiguration = settingKey[JS.Object]("The full configuration object")

    //////////////////
    // OPTIMIZATION //
    //////////////////

    val requireOptimization = settingKey[JS.Object]("The full optimization object")
    val requireOptimized = settingKey[Boolean]("If true an r.js optimization state is added to the pipeline")
    val requireMinified = settingKey[Boolean]("If true the minified versions of modules in paths are used")
    val requireCDN = settingKey[Boolean]("If true the CDN versions of modules in paths are used")

    ////////////////
    // GENERATION //
    ////////////////

    val requireVersion = settingKey[String]("The require js version")
    val requirePath = settingKey[String]("The web jars require js path")
    val requireDirectory = settingKey[File]("The main file directory")
    val requireMainModule = settingKey[String]("The main module name")
    val requireMainPath = settingKey[String]("The main file path")
    val requireMainTemplateFile = settingKey[Option[File]]("The main template file")
    val requireMainFile = settingKey[File]("The main file")
    val requireMainGenerator = taskKey[Seq[File]]("Generate the config file")
    val requireIncludeFilter = settingKey[FileFilter]("The include filter generated from paths")
    val requireExcludeFilter = settingKey[FileFilter]("The exclude filter generated from paths")
    val requireCallbackModule = settingKey[RequireId]("The callback module name")

    lazy val unscopedProjectSettings = Seq(

      ///////////////////
      // CONFIGURATION //
      ///////////////////

      requireConfigurationBaseUrl := None,
      requireConfigurationPaths := Seq(
        requireMainModule.value -> RequirePath.minify(requireMainModule.value, requireMinified.value)
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
      requireConfigurationCallback := Some(
        JavaScript(s"""function() { require(['${requireCallbackModule.value}']); }""")
      ),
      requireConfigurationEnforceDefine := false,
      requireConfigurationXhtml := false,
      requireConfigurationUrlArgs := None,
      requireConfigurationScriptType := "text/javascript",
      requireConfigurationSkipDataMain := false,
      requireConfiguration := JS.Object(
        "baseUrl" -> requireConfigurationBaseUrl.value,
        "paths" -> JS.Object(requireConfigurationPaths.value.map {
          case (id, path) =>
            id -> JS(path) } : _*
        ),
        "bundles" -> JS.Object(requireConfigurationBundles.value.map {
          case (id, bundle) =>
            id -> JS(bundle)
        }: _*),
        "shim" -> JS.Object(requireConfigurationShim.value.map {
          case (id, RequireShimConfig(deps, exports, init)) =>
            id -> JS.Object(
              "deps" -> deps,
              "exports" ->  exports,
              "init" -> init
            )
        }:_*),
        "map" -> requireConfigurationMap.value.groupBy(_._1).toSeq.map {
          case (k, v) =>
            k -> v.unzip._2.reduce(_ ++ _)
        },
        "config" -> JS.Object(requireConfigurationConfig.value: _*),
        "packages" -> requireConfigurationPackages.value.map {
          case (id, _package) =>
            id -> JS.Object(
              "name" -> _package.name,
              "main" -> _package.main
            )
        },
        "nodeIdCompat" -> requireConfigurationNodeIdCompat.value,
        "waitSeconds" -> requireConfigurationWaitSeconds.value,
        "context" -> requireConfigurationContext.value,
        "deps" -> requireConfigurationDeps.value,
        "callback" -> requireConfigurationCallback.value,
        "enforceDefine" -> requireConfigurationEnforceDefine.value,
        "xhtml" -> requireConfigurationXhtml.value,
        "urlArgs" -> requireConfigurationUrlArgs.value,
        "scriptType" -> requireConfigurationScriptType.value,
        "skipDataMain" -> requireConfigurationSkipDataMain.value
      ),

      //////////////////
      // OPTIMIZATION //
      //////////////////

      requireOptimization := JS.Object(),
      requireOptimized := true,
      requireMinified := true,
      requireCDN := true,

      ////////////////
      // GENERATION //
      ////////////////
      
      requireDirectory := sourceManaged.value / "require-js",
      requirePath := {
        val path = if (requireCDN.value)
          s"//cdn.jsdelivr.net/webjars/requirejs/${requireVersion.value}/require"
        else
          s"/${webModulesLib.value}/requirejs/require"
        RequirePath.filename(RequirePath.minify(path, requireMinified.value))
      },
      requireMainModule := "main",
      requireMainPath := RequirePath.minify(
        requireConfigurationBaseUrl.value.getOrElse("") + "/" + requireMainModule.value,
        requireMinified.value
      ),
      requireMainTemplateFile := None,
      requireMainFile := requireDirectory.value / RequirePath.filename(RequirePath.relativize(requireMainPath.value)),
      requireMainGenerator := {
        implicit val logger = streams.value.log
        val configuration = requireConfiguration.value.js
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
          attributes = Map("data-main" -> RequirePath.filename(requireMainPath.value))
        )
      )
    )
  }

  import autoImport._

  val rjsSettings = Seq(
    RjsKeys.version := requireVersion.value,
    RjsKeys.baseUrl := (requireConfigurationBaseUrl in Assets).value.map(RequirePath.relativize).getOrElse("."),
    RjsKeys.mainConfig := RequirePath.minify((requireMainModule in Assets).value, (requireMinified in Assets).value),
    RjsKeys.mainConfigFile := new File(RequirePath.relativize(s"${RjsKeys.baseUrl.value}/${RequirePath.filename(RjsKeys.mainConfig.value)}")),
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

  private[SbtRequire] def toJSObject(key: String, seq: Seq[(String, JS[_])])(implicit logger: Logger): (String, JS.Object) = {
    seq.groupBy(_._1) foreach {
      case (groupKey, groupValues) =>
        if (groupValues.size > 1)
          logger.warn(s"duplicate key '$groupKey' in '$key!")
    }
    (key, JS.Object(seq: _*))
  }
}