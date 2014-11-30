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

object SbtAngularUiBootstrap
  extends AutoPlugin {
  override lazy val requires = SbtBootstrapJs && SbtAngularJs

  object autoImport {
    val angularUiBootstrapVersion = settingKey[String]( "Angular Ui Bootstrap version" )
  }

  import com.byteground.sbt.SbtAngularUiBootstrap.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths += "angular-ui-bootstrap" -> s"${webModulesLib.value}/angular-ui-bootstrap/ui-bootstrap-tpls",
    requireJsConfigurationShim += "angular-ui-bootstrap" -> RequireJsConfiguration.Shim.Config(
      Seq( "angular" ),
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

  override lazy val projectSettings = Seq(
    angularUiBootstrapVersion := "0.12.0",
    libraryDependencies += "org.webjars" % "angular-ui-bootstrap" % angularUiBootstrapVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}