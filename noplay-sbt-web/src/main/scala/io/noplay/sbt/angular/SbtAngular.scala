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
package io.noplay.sbt.angular

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import io.alphard.sbt.util.Javascript
import io.noplay.sbt.SbtRequire
import io.noplay.sbt.SbtRequire.autoImport.RequireConfiguration.Shim
import io.noplay.sbt.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngular
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val angularVersion = settingKey[String]("Angular version")
  }

  import SbtAngular.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths +=
      "angular" -> s"/${webModulesLib.value}/angularjs/angular"
    ,
    requireConfigurationShim +=
      "angular" -> Shim.Config(exports = Some("angular"))
  ) ++
    angularModule("animate", "ngAnimate") ++
    angularModule("aria", "ngAria") ++
    angularModule("cookies", "ngCookies") ++
    angularModule("loader", "ngLoader") ++
    angularModule("message-format", "ngMessageFormat") ++
    angularModule("messages", "ngMessages") ++
    angularModule("mocks", "ngMocks") ++
    angularModule("resource", "ngResource") ++
    angularModule("route", "ngRoute") ++
    angularModule("sanitize", "ngSanitize") ++
    angularModule("scenario", "ngScenario") ++
    angularModule("touch", "ngTouch")

  def angularModule(name: String, moduleName: String) = {
    val fullName = s"angular-$name"
    Seq(
      requireConfigurationPaths +=
        fullName -> s"/${webModulesLib.value}/angularjs/$fullName",
      requireConfigurationShim +=
        fullName -> RequireConfiguration.Shim.Config(
          Seq("angular"),
          init = Some(
            Javascript.Function(
              s"""function(angular) {
                  |  return angular.module("$moduleName");
                  |}
          """.stripMargin
            )
          )
        )
    )
  }

  override lazy val projectSettings = Seq(
    angularVersion := "1.5.5",
    libraryDependencies += "org.webjars.bower" % "angularjs" % angularVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}