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
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngularTranslate
  extends AutoPlugin {
  override val requires = SbtAngular

  object autoImport {
    val angularTranslateVersion = settingKey[String]("Angular Translate version")
  }

  import SbtAngularTranslate.autoImport._

  val unscopedProjectSettings = Seq(
    requireMainConfigPaths ++= Seq(
      "angular-translate" -> s"/${webModulesLib.value}/angular-translate/dist/angular-translate",
      path(webModulesLib.value, "handler-log"),
      path(webModulesLib.value, "interpolation-messageformat"),
      path(webModulesLib.value, "loader-partial"),
      path(webModulesLib.value, "loader-static-files"),
      path(webModulesLib.value, "loader-url"),
      path(webModulesLib.value, "storage-cookie"),
      path(webModulesLib.value, "storage-local")
    ),
    requireMainConfigShim ++= Seq(
      "angular-translate" -> RequireShimConfig(
        Seq("angular"),
        init = Some(
          JavaScript(
            """function(angular) {
              |  return angular.module("pascalprecht.translate");
              |}
            """.stripMargin
          )
        )
      ),
      "angular-translate-handler-log" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-interpolation-messageformat" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-loader-partial" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-loader-static-files" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-loader-url" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-storage-cookie" -> RequireShimConfig(Seq("angular-translate")),
      "angular-translate-storage-local" -> RequireShimConfig(Seq("angular-translate"))
    )
  )

  override val projectSettings = Seq(
    angularTranslateVersion := "2.11.0",
    libraryDependencies ++= Seq(
      "org.webjars.npm" % "angular-translate" % angularTranslateVersion.value
    )
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)

  private def path(libPath: String, suffix: String) =
    s"angular-translate-$suffix" -> s"/$libPath/angular-translate/dist/angular-translate-$suffix/angular-translate-$suffix"
}