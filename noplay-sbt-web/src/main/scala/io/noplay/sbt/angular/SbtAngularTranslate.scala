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
import io.noplay.sbt.SbtRequire.autoImport.RequireConfiguration.Shim
import io.noplay.sbt.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngularTranslate
  extends AutoPlugin {
  override val requires = SbtAngular

  object autoImport {
    val angularTranslateVersion = settingKey[String]( "Angular Translate version" )
  }

  import SbtAngularTranslate.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths ++= Seq(
      "angular-translate" -> s"/${webModulesLib.value}/angular-translate/angular-translate",
      "angular-translate-loader-partial" -> s"/${webModulesLib.value}/angular-translate-loader-partial/angular-translate-loader-partial"
    ),
    requireConfigurationShim ++= Seq(
      "angular-translate" -> Shim.Config(
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
      "angular-translate-loader-partial" -> RequireConfiguration.Shim.Config(
        Seq("angular-translate")
      )
    )
  )

  override val projectSettings = Seq(
    angularTranslateVersion := "2.7.0",
    libraryDependencies ++= Seq(
      "org.webjars" % "angular-translate" % angularTranslateVersion.value,
      "org.webjars" % "angular-translate-loader-partial" % angularTranslateVersion.value
    )
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}