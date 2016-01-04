/**
 * Copyright © 2009-2016 ByTeGround, Inc
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

import com.byteground.sbt.SbtRequire.autoImport._
import com.byteground.sbt.util.Javascript
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

object SbtAngularGoogleMaps
  extends AutoPlugin {
  override lazy val requires = SbtAngular

  object autoImport {
    val angularGoogleMapsVersion = settingKey[String]("Angular Google Maps version")
  }

  import com.byteground.sbt.SbtAngularGoogleMaps.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths ++= Seq(
      "lodash" -> "lib/lodash/lodash",
      "angular-google-maps" -> "lib/angular-google-maps/angular-google-maps"
    ),
    requireConfigurationShim +=
      "angular-google-maps" -> RequireConfiguration.Shim.Config(
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
    angularGoogleMapsVersion := "2.1.1",
    libraryDependencies ++= Seq(
      "org.webjars" % "lodash" % "3.3.1",
      "org.webjars" % "angular-google-maps" % angularGoogleMapsVersion.value exclude("org.webjars", "lodash")
    )
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}