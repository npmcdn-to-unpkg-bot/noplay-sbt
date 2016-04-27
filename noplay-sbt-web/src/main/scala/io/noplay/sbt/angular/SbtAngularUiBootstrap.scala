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
import io.noplay.sbt.SbtRequire.autoImport.RequireConfiguration.Shim
import io.noplay.sbt.SbtRequire.autoImport._
import io.noplay.sbt.bootstrap.SbtBootstrap
import sbt.Keys._
import sbt._

object SbtAngularUiBootstrap
  extends AutoPlugin {
  override val requires = SbtBootstrap && SbtAngular

  object autoImport {
    val angularUiBootstrapVersion = settingKey[String]("Angular Ui Bootstrap version")
  }

  import SbtAngularUiBootstrap.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "angular-ui-bootstrap" -> s"/${webModulesLib.value}/angular-ui-bootstrap/ui-bootstrap-tpls",
    requireConfigurationShim += "angular-ui-bootstrap" -> Shim.Config(
      Seq("angular"),
      init = Some(
        Javascript.Function(
          """function(angular) {
            |  return angular.module("ui.bootstrap");
            |}
          """.stripMargin
        )
      )
    )
  )

  override val projectSettings = Seq(
    angularUiBootstrapVersion := "0.13.4",
    libraryDependencies += "org.webjars" % "angular-ui-bootstrap" % angularUiBootstrapVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}