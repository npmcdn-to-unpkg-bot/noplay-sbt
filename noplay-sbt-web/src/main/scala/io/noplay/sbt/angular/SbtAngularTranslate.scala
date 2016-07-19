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
      "angular-translate" -> s"/${webModulesLib.value}/angular-translate/dist/angular-translate",
      path(webModulesLib.value, "handler-log"),
      path(webModulesLib.value, "interpolation-messageformat"),
      path(webModulesLib.value, "loader-partial"),
      path(webModulesLib.value, "loader-static-files"),
      path(webModulesLib.value, "loader-url"),
      path(webModulesLib.value, "storage-cookie"),
      path(webModulesLib.value, "storage-local")
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
      "angular-translate-handler-log" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-interpolation-messageformat" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-loader-partial" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-loader-static-files" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-loader-url" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-storage-cookie" -> RequireConfiguration.Shim.Config(Seq("angular-translate")),
      "angular-translate-storage-local" -> RequireConfiguration.Shim.Config(Seq("angular-translate"))
    )
  )

  override val projectSettings = Seq(
    angularTranslateVersion := "2.11.0",
    libraryDependencies ++= Seq(
      "org.webjars.npm" % "angular-translate" % angularTranslateVersion.value
    )
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )

  private def path(libPath: String, suffix: String) =
    s"angular-translate-$suffix" -> s"/$libPath/angular-translate/dist/angular-translate-$suffix/angular-translate-$suffix"
}