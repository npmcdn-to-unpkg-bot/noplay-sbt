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
package io.noplay.sbt.web.nunjucks

import com.typesafe.sbt.jse.SbtJsTask
import com.typesafe.sbt.jse.SbtJsTask.autoImport.JsTaskKeys._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.js.{JS, JavaScript}
import io.alphard.sbt.SbtNpm.autoImport._
import io.alphard.sbt._
import io.noplay.sbt.web.require.SbtRequire
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtNunjucks
  extends AutoPlugin {

  override val requires = SbtNpm && SbtJsTask && SbtRequire

  override val trigger = AllRequirements

  object autoImport {
    val nunjucksVersion = settingKey[String]("Nunjucks version")
    val nunjucksIncludeFilter = settingKey[FileFilter]("Nunjucks include filter")
    val nunjucksExcludeFilter = settingKey[FileFilter]("Nunjucks exclude filter")
    val nunjucksPrecompile = settingKey[Boolean]("Nunjucks precompile")
    val nunjucksBaseUrl = settingKey[Option[String]]("Nunjucks base url")
    val nunjucksOptions = settingKey[JS.Object]("Nunjucks precompile options")
    val nunjucks = taskKey[Seq[File]]("Nunjucks compile templates")
  }

  import SbtNunjucks.autoImport._

  override lazy val buildSettings =
    inTask(nunjucks)(
      SbtJsTask.jsTaskSpecificUnscopedBuildSettings ++
        Seq(
          moduleName := "nunjucks",
          shellFile := getClass.getResource("/io/noplay/sbt/web/nunjucks/nunjucks.js")
        )
    )

  override lazy val projectSettings = Seq(
    nunjucksVersion := "2.4.2",
    nunjucksPrecompile := false,
    nunjucksOptions := JS.Object.empty,
    libraryDependencies += ("org.webjars.npm" % "nunjucks" % nunjucksVersion.value).intransitive(),
    npmDevDependencies ++= Seq(
      "mkdirp" -> ">=0.5.1",
      "nunjucks" -> nunjucksVersion.value
    )
  ) ++
    inConfig(Assets)(unscopedSettings) ++
    inConfig(TestAssets)(unscopedSettings) ++
    inTask(nunjucks)(
      SbtJsTask.jsTaskSpecificUnscopedProjectSettings ++
        inConfig(Assets)(nunjucksUnscopedSettings) ++
        inConfig(TestAssets)(nunjucksUnscopedSettings) ++ Seq(
        taskMessage in Assets := s"Nunjucks ${if (nunjucksPrecompile.value) "precompiling" else "copying"}",
        taskMessage in TestAssets := s"Nunjucks test ${if (nunjucksPrecompile.value) "precompiling" else "copying"}"
      )
    ) ++ SbtJsTask.addJsSourceFileTasks(nunjucks) ++ Seq(
    nunjucks in Assets := {
      if (nunjucksPrecompile.value) (
        nunjucks in Assets).dependsOn(nodeModules in Assets).value
      else
        Def.task[Seq[File]](Seq.empty).value
    },
    nunjucks in TestAssets := {
      if (nunjucksPrecompile.value) (
        nunjucks in TestAssets).dependsOn(nodeModules in TestAssets).value
      else
        Def.task[Seq[File]](Seq.empty).value
    },
    nodeModuleDirectories in Plugin += npmModulesDirectory.value
  )

  private lazy val nunjucksUnscopedSettings = Seq(
    includeFilter <<= nunjucksIncludeFilter,
    excludeFilter <<= nunjucksExcludeFilter,
    jsOptions := (nunjucksOptions.value ++ JS.Object(
      "baseUrl" -> nunjucksBaseUrl.value
    )).js
  )

  private lazy val unscopedSettings = Seq(
    nunjucksIncludeFilter := "*.njk",
    nunjucksExcludeFilter := NothingFilter,
    nunjucksBaseUrl := requireMainConfigBaseUrl.value,
    excludeFilter := {
      if (nunjucksPrecompile.value)
        excludeFilter.value || nunjucksIncludeFilter.value
      else
        excludeFilter.value
    },
    requireMainConfigPaths += "nunjucks" -> RequirePath(
      s"/${webModulesLib.value}/nunjucks/browser/nunjucks${if (nunjucksPrecompile.value) "-slim" else ""}",
      s"//cdnjs.cloudflare.com/ajax/libs/nunjucks/${nunjucksVersion.value}/nunjucks${if (nunjucksPrecompile.value) "-slim" else ""}"
    ).minify(".min"),
    requireMainConfigShim += "nunjucks" -> RequireShimConfig(
      deps = Seq("module"),
      init = Some(
        JavaScript(
          s"""function(module) {
              |  var config = module.config && module.config() || {};
              |  nunjucks.configure(config);
              |  return nunjucks;
              |}
          """.stripMargin
        )
      ),
      exports = Some("nunjucks")
    )
  )

}