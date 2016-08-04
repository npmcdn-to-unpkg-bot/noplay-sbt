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
import io.noplay.sbt.web.bootstrap.SbtBootstrap
import io.noplay.sbt.web.require.SbtRequire.autoImport._
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
    requireConfigurationPaths += "angular-ui-bootstrap" -> s"/${webModulesLib.value}/angular-ui-bootstrap/dist/ui-bootstrap-tpls",
    requireConfigurationShim += "angular-ui-bootstrap" -> RequireShimConfig(
      Seq("angular"),
      init = Some(
        JavaScript(
          """function(angular) {
            |  return angular.module("ui.bootstrap");
            |}
          """.stripMargin
        )
      )
    )
  )

  override val projectSettings = Seq(
    angularUiBootstrapVersion := "1.3.3",
    libraryDependencies += "org.webjars.npm" % "angular-ui-bootstrap" % angularUiBootstrapVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}