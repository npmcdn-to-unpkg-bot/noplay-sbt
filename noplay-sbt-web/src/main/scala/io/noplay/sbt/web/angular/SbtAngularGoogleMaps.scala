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
import io.alphard.sbt.util.Javascript
import io.noplay.sbt.web.lodash.SbtLodash
import io.noplay.sbt.web.require.SbtRequire
import SbtRequire.autoImport.RequireConfiguration.Shim
import SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtAngularGoogleMaps
  extends AutoPlugin {
  override val requires = SbtAngular && SbtLodash

  object autoImport {
    val angularGoogleMapsVersion = settingKey[String]("Angular Google Maps version")
  }

  import SbtAngularGoogleMaps.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths ++= Seq(
      "angular-google-maps" -> s"/${webModulesLib.value}/angular-google-maps/dist/angular-google-maps"
    ),
    requireConfigurationShim +=
      "angular-google-maps" -> Shim.Config(
        deps = Seq("angular", "lodash"),
        init = Some(
          Javascript.Function(
            """function(angular) {
              |  return angular
              |    .module('uiGmapgoogle-maps')
              |    .config(['$provide', function ($provide) {
              |       $provide
              |       .decorator("uiGmapMapScriptLoader",
              |         ["$delegate", "$q", function ($delegate, $q) {
              |            return {
              |              load: function (options) {
              |                return $q(function (resolve, reject) {
              |                  require(["google/maps"], resolve, reject);
              |                });
              |              }
              |            };
              |         }]);
              |    }]);
              |}
            """.stripMargin
          )
        )
      )
  )

  override lazy val projectSettings = Seq(
    angularGoogleMapsVersion := "2.3.3",
    libraryDependencies ++= Seq(
      "org.webjars.npm" % "angular-google-maps" % angularGoogleMapsVersion.value exclude("org.webjars.npm", "lodash")
    )
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}