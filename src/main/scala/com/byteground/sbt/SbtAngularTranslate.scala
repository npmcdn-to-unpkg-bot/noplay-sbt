/**
 * Copyright © 2009-2015 ByTeGround, Inc
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

object SbtAngularTranslate
  extends AutoPlugin {
  override lazy val requires = SbtAngularJs

  object autoImport {
    val angularTranslateVersion = settingKey[String]( "Angular Translate version" )
  }

  import com.byteground.sbt.SbtAngularTranslate.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths ++= Seq(
      "angular-translate" -> s"${webModulesLib.value}/angular-translate/angular-translate",
      "angular-translate-loader-partial" -> s"${webModulesLib.value}/angular-translate-loader-partial/angular-translate-loader-partial"
    ),
    requireJsConfigurationShim ++= Seq(
      "angular-translate" -> RequireJsConfiguration.Shim.Config(
        Seq("angular"),
        init = Some(
          Javascript.Function(
            """function(angular) {
              |  return angular.module("pascalprecht.translate");
              |}
            """.stripMargin
          )
        )
      ),
      "angular-translate-loader-partial" -> RequireJsConfiguration.Shim.Config(
        Seq("angular-translate")
      )
    )
  )

  override lazy val projectSettings = Seq(
    angularTranslateVersion := "2.4.2",
    libraryDependencies ++= Seq(
      "org.webjars" % "angular-translate" % angularTranslateVersion.value,
      "org.webjars" % "angular-translate-loader-partial" % angularTranslateVersion.value
    )
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}