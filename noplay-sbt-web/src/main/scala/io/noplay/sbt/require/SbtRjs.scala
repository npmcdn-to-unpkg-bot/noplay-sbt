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
package io.noplay.sbt.require

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.Charset

import com.typesafe.sbt.jse.{SbtJsEngine, SbtJsTask}
import com.typesafe.sbt.web.SbtWeb
import com.typesafe.sbt.web.SbtWeb.autoImport.WebJs._
import com.typesafe.sbt.web.pipeline.Pipeline
import io.alphard.sbt.SbtNpm
import io.alphard.sbt.SbtNpm.autoImport._
import sbt.Keys._
import sbt._

import scala.collection.immutable.SortedMap

object SbtRjs
  extends AutoPlugin {

  override def requires = SbtJsTask && SbtNpm

  override def trigger = AllRequirements

  object autoImport {
    val rjs = TaskKey[Pipeline.Stage]("rjs", "Perform RequireJs optimization on the asset pipeline.")

    object RjsKeys {
      val version = SettingKey[String]("rjs-version", "The rjs version")
      val appBuildProfile = TaskKey[JS.Object]("rjs-app-build-profile", "The project build profile contents.")
      val appDir = SettingKey[File]("rjs-app-dir", "The top level directory that contains your app js files. In effect, this is the source folder that rjs reads from.")
      val baseUrl = TaskKey[String]("rjs-base-url", """The dir relative to the source assets or public folder where js files are housed. Will default to "js", "javascripts" or "." with the latter if the other two cannot be found.""")
      val buildProfile = TaskKey[JS.Object]("rjs-build-profile", "Build profile key -> value settings in addition to the defaults supplied by appBuildProfile. Any settings in here will also replace any defaults.")
      val buildWriter = TaskKey[JavaScript]("rjs-build-writer", "The project build writer JavaScript that is responsible for writing out source files in rjs.")
      val dir = SettingKey[File]("rjs-dir", "By default, all modules are located relative to this path. In effect this is the target directory for rjs.")
      val generateSourceMaps = SettingKey[Boolean]("rjs-generate-source-maps", "By default, source maps are generated.")
      val mainConfig = SettingKey[String]("rjs-main-config", "By default, 'main' is used as the module for configuration.")
      val mainConfigFile = TaskKey[File]("rjs-main-config-file", "The full path to the main configuration file.")
      val mainModule = SettingKey[String]("rjs-main-module", "By default, 'main' is used as the module.")
      val modules = SettingKey[Seq[JS.Object]]("rjs-modules", "The json array of modules.")
      val optimize = SettingKey[String]("rjs-optimize", "The name of the optimizer, defaults to uglify2.")
      val paths = TaskKey[Map[String, (String, String)]]("rjs-paths", "RequireJS path mappings of module ids to a tuple of the build path and production path. By default all WebJar libraries are made available from a CDN and their mappings can be found here (unless the cdn is set to None).")
      val preserveLicenseComments = SettingKey[Boolean]("rjs-preserve-license-comments", "Whether to preserve comments or not. Defaults to false given source maps (see http://requirejs.org/docs/errors.html#sourcemapcomments).")
      val removeCombined = SettingKey[Boolean]("rjs-remove-combined", "Whether to remove source files. Defaults to true.")
      val webJarCdnPatterns = SettingKey[Map[String, String]]("rjs-webjar-cdns",
        """A map of (organization, patterns) to be used for locating WebJars on public CDNs.
          |By default classic web jars CDN pattern is //cdn.jsdelivr.net/webjars/{name}/{revision}/{path}.
          |By default npm web jars CDN pattern is "//npmcdn.com/{name}@{revision}/{path}".
          |By default bower web jars CDN pattern is "//bowercdn.net/c/{name}-{revision}/{path}"
          |The {name} is replaced with the module name.
          |The {revision} is replaced with the module revision.
          |The {path} is replaced with the module relative path from the webLib settings.""".stripMargin
      )
    }
  }

  import SbtJsEngine.autoImport.JsEngineKeys._
  import SbtJsTask.autoImport.JsTaskKeys._
  import SbtWeb.autoImport._
  import WebKeys._
  import autoImport._
  import RjsKeys._

  override def projectSettings = Seq(
    RjsKeys.version := "2.2.0",
    npmDevDependencies += "requirejs" -> RjsKeys.version.value,
    appBuildProfile := getAppBuildProfile.value,
    appDir := (resourceManaged in rjs).value / "appdir",
    baseUrl := getBaseUrl.value,
    buildProfile := JS.Object.empty,
    buildWriter := getBuildWriter.value,
    dir := (resourceManaged in rjs).value / "build",
    excludeFilter in rjs := HiddenFileFilter,
    generateSourceMaps := true,
    includeFilter in rjs := GlobFilter("*.js") | GlobFilter("*.css") | GlobFilter("*.map"),
    mainConfig := mainModule.value,
    mainConfigFile := new File(baseUrl.value, mainConfig.value + ".js"),
    mainModule := "main",
    modules := Seq(JS.Object("name" -> mainModule.value)),
    optimize := "uglify2",
    paths := getWebJarPaths.value,
    preserveLicenseComments := false,
    removeCombined := true,
    resourceManaged in rjs := webTarget.value / rjs.key.label,
    rjs := runOptimizer.dependsOn(webJarsNodeModules in Plugin).value,
    webJarCdnPatterns := Map(
      "org.webjars" -> "//cdn.jsdelivr.net/webjars/{name}/{revision}/{path}",
      "org.webjars.npm" -> "//npmcdn.com/{name}@{revision}/{path}",
      "org.webjars.bower" -> "//bowercdn.net/c/{name}-{revision}/{path}"
    )
  )


  val Utf8 = Charset.forName("UTF-8")

  private def getAppBuildProfile: Def.Initialize[Task[JS.Object]] = Def.task {
    JS.Object(
      "appDir" -> appDir.value,
      "baseUrl" -> baseUrl.value,
      "dir" -> dir.value,
      "generateSourceMaps" -> generateSourceMaps.value,
      "mainConfigFile" -> appDir.value / mainConfigFile.value.getPath,
      "modules" -> modules.value,
      "onBuildWrite" -> buildWriter.value,
      "optimize" -> optimize.value,
      "paths" -> paths.value.map(m => m._1 -> "empty:"),
      "preserveLicenseComments" -> preserveLicenseComments.value,
      "removeCombined" -> removeCombined.value
    ) ++ buildProfile.value
  }

  private def getBaseUrl: Def.Initialize[Task[String]] = Def.task {
    def dirIfExists(dir: String): Option[String] = {
      val dirPath = dir + java.io.File.separator
      if ((mappings in Assets).value.exists(m => m._2.startsWith(dirPath))) {
        Some(dir)
      } else {
        None
      }
    }
    dirIfExists("js").orElse(dirIfExists("javascripts")).getOrElse(".")
  }

  private def getBuildWriter: Def.Initialize[Task[JavaScript]] = Def.task {
    val source = getResourceAsList("io/noplay/sbt/require/buildWriter.js")
      .to[Vector]
      .dropRight(1) :+ s"""})(
          ${JS(unixPath(mainConfigFile.value.toString))},
          ${JS(paths.value.map(e => e._2._1 -> e._2._2))}
          )"""
    JavaScript(source.mkString("\n"))
  }

  private def getResourceAsList(name: String): List[String] = {
    val in = SbtRjs.getClass.getClassLoader.getResourceAsStream(name)
    val reader = new BufferedReader(new InputStreamReader(in, Utf8))
    try {
      IO.readLines(reader)
    } finally {
      reader.close()
    }
  }

  private def getWebJarPaths: Def.Initialize[Task[Map[String, (String, String)]]] = Def.task {
    /*
     * The idea here is to only declare WebJar CDNs where the user has explicitly declared
     * maths in their main configuration js file. The result of this is that rjs will process only
     * what it really needs to process as opposed to an alternate strategy where all WebJar
     * js files are made known to it (which is slow).
     */
    val maybeMainConfigFile = (mappings in Assets).value.find(_._2 == mainConfigFile.value.getPath).map(_._1)
    maybeMainConfigFile.fold(Map[String, (String, String)]()) { f =>
      val lib = unixPath(withSep(webModulesLib.value))
      val config = IO.read(f, Utf8)
      val pathModuleMappings = SortedMap(
        s"""['"]?([^\\s'"]*)['"]?\\s*:\\s*[\\[]?.*['"].*/$lib(.*)['"]""".r
          .findAllIn(config)
          .matchData.map(m => m.subgroups(1) -> m.subgroups(0))
          .toIndexedSeq
          : _*
      )
      val webJarLocalPathPrefix = withSep((webJarsDirectory in Assets).value.getPath) + lib
      val webJarRelPaths = (webJars in Assets).value.map(f => unixPath(f.getPath.drop(webJarLocalPathPrefix.size))).toSet
      def minifiedModulePath(p: String): String = {
        def ifExists(p: String): Option[String] = if (webJarRelPaths.contains(p + ".js")) Some(p) else None
        ifExists(p + ".min").orElse(ifExists(p + "-min")).getOrElse(p)
      }
      val webJarCdnPaths = for {
        m <- allDependencies(update.value)
        cdnPattern <- webJarCdnPatterns.value.get(m.organization)
      } yield for {
        pm <- pathModuleMappings.from(m.name + "/") if pm._1.startsWith(m.name + "/")
      } yield {
        val (moduleIdPath, moduleId) = pm
        val moduleIdRelPath = minifiedModulePath(moduleIdPath).drop(m.name.size + 1)
        val cdnPath = cdnPattern.replace("{name}", m.name).replace("{revision}", m.revision).replace("{path}", moduleIdRelPath)
        moduleId -> (lib + moduleIdPath, cdnPath)
      }
      webJarCdnPaths.flatten.toMap
    }
  }

  private def allDependencies(updateReport: UpdateReport): Seq[ModuleID] = {
    updateReport.filter(
      configurationFilter(Compile.name) && artifactFilter(`type` = "jar")
    ).toSeq.map(_._2).distinct
  }

  private def runOptimizer: Def.Initialize[Task[Pipeline.Stage]] = Def.task {
    mappings =>

      val include = (includeFilter in rjs).value
      val exclude = (excludeFilter in rjs).value
      val optimizerMappings = mappings.filter(f => !f._1.isDirectory && include.accept(f._1) && !exclude.accept(f._1))
      SbtWeb.syncMappings(
        streams.value.cacheDirectory,
        "rjs-cache",
        optimizerMappings,
        appDir.value
      )

      val targetBuildProfileFile = (resourceManaged in rjs).value / "app.build.js"
      IO.write(targetBuildProfileFile, appBuildProfile.value.js, Utf8)

      val cacheDirectory = streams.value.cacheDirectory / rjs.key.label
      val runUpdate = FileFunction.cached(cacheDirectory, FilesInfo.hash) {
        _ =>
          streams.value.log.info("Optimizing JavaScript with RequireJS")

          SbtJsTask.executeJs(
            state.value,
            (engineType in rjs).value,
            (command in rjs).value,
            Nil,
            npmModulesDirectory.value / "requirejs" / "bin" / "r.js",
            Seq("-o", targetBuildProfileFile.getAbsolutePath),
            (timeoutPerSource in rjs).value * optimizerMappings.size
          )

          dir.value.***.get.toSet
      }

      val optimizedMappings = runUpdate(appDir.value.***.get.toSet).filter(_.isFile).pair(relativeTo(dir.value))
      (mappings.toSet -- optimizerMappings.toSet ++ optimizedMappings).toSeq
  }

  private def withSep(p: String): String = if (p.endsWith(java.io.File.separator)) p else p + java.io.File.separator

  /**
    * Replaces \ -> / so that paths are in UNIX style.
    * Windows understands them too and as a bonus we have no need to escape them in Regex.
    * Also / used URIs and using same separator makes various operations on URIs and paths more reliable.
    *
    * @param p path
    * @return
    */
  private def unixPath(p: String): String = p.replace("\\","/")

}