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
import io.noplay.sbt.require.SbtRequire
import SbtRequire.autoImport.RequireConfiguration.Shim
import SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngularUiRouter
  extends AutoPlugin {
  override val requires = SbtAngular

  object autoImport {
    val angularUiRouterVersion = settingKey[String]("Angular UI Router version")
  }

  import SbtAngularUiRouter.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "angular-ui-router" -> s"/${webModulesLib.value}/angular-ui-router/release/angular-ui-router",
    requireConfigurationShim += "angular-ui-router" -> Shim.Config(
      Seq("angular"),
      init = Some(
        Javascript.Function(
          """function(angular) {
            |  return angular
            |    .module("ui.router.compat")
            |    .config(["$provide", function ($provide) {
            |      $provide.decorator("$templateFactory", ["$templateRequest", "$delegate", function ($templateRequest, $delegate) {
            |        $delegate.fromUrl = $templateRequest;//override
            |        return $delegate;
            |      }]);
            |    }]);
            |}
          """.stripMargin
        )
      )
    )
  )

  override val projectSettings = Seq(
    angularUiRouterVersion := "1.0.0-beta.5",
    libraryDependencies += "org.webjars.npm" % "angular-ui-router" % angularUiRouterVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}