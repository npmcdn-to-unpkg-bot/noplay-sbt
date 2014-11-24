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

import com.byteground.sbt.SbtRequireJs.autoImport._
import com.byteground.sbt.util.Javascript
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

object SbtAngularUiRouter
  extends AutoPlugin {
  override lazy val requires = SbtAngularJs

  object autoImport {
    val angularUiRouterVersion = settingKey[String]( "Angular Ui Router version" )
  }

  import com.byteground.sbt.SbtAngularUiRouter.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths += "angular-ui-router" -> s"${webModulesLib.value}/angular-ui-router/angular-ui-router",
    requireJsConfigurationShim += "angular-ui-router" -> RequireJsConfiguration.Shim.Config(
      Seq( "angular" ),
      init = Some(
        Javascript.Function(
          """function(angular) {
            |  return angular.module("ui.router.compat");
            |}
          """.stripMargin
        )
      )
    )
  )

  override lazy val projectSettings = Seq(
    angularUiRouterVersion := "0.2.11",
    libraryDependencies += "org.webjars" % "angular-ui-router" % angularUiRouterVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}