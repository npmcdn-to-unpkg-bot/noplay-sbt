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
package io.noplay.sbt.web.angular

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.js.JavaScript
import io.noplay.sbt.web.require.SbtRequire
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngular
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val angularVersion = settingKey[String]("AngularJS version")
  }

  import SbtAngular.autoImport._

  val unscopedProjectSettings = Seq(
    requireMainConfigPaths += "angular" -> path(webModulesLib.value, "angular"),
    requireMainConfigShim += "angular" -> RequireShimConfig(exports = Some("angular"))
  )

  override lazy val projectSettings = Seq(
    angularVersion := "1.5.8",
    libraryDependencies += module("angular", angularVersion.value)
  ) ++
    inConfig(Assets)(unscopedProjectSettings) ++
    inConfig(TestAssets)(unscopedProjectSettings) ++
    angularSettings("animate") ++
    angularSettings("aria") ++
    angularSettings("cookies") ++
    //module("loader") ++
    //module("message-format") ++
    angularSettings("messages") ++
    angularSettings("mocks") ++
    angularSettings("resource") ++
    angularSettings("route") ++
    angularSettings("sanitize") //++
  //module("scenario") ++
  //module("touch")

  private def dashToCamelcase(name: String) =
    "-([a-z\\d])".r.replaceAllIn(name, { m => m.group(1).toUpperCase() })

  private def angularSettings(suffix: String = "") = {
    val name = "angular-" + suffix
    val angularName = dashToCamelcase("ng-" + suffix)
    val unscopedSettings = Seq(
      requireMainConfigPaths +=
        name -> path(webModulesLib.value, name),
      requireMainConfigShim +=
        name -> RequireShimConfig(
          Seq("angular"),
          init = Some(
            JavaScript(
              s"""function(angular) {
                  |  return angular.module('$angularName');
                  |}
          """.stripMargin
            )
          )
        )
    )

    Seq(
      libraryDependencies += module(name, angularVersion.value)
    ) ++
      inConfig(Assets)(unscopedSettings) ++
      inConfig(TestAssets)(unscopedSettings)
  }

  private def module(name: String, version: String) = "org.webjars.npm" % name % version

  private def path(libPath: String, name: String) = s"/$libPath/$name/$name"
}