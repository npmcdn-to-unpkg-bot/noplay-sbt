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
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

object SbtAngularMaterial
  extends AutoPlugin {
  override val requires = SbtAngular

  object autoImport {
    val angularMaterialVersion = settingKey[String]("Angular Material version")
  }

  import com.byteground.sbt.SbtAngularMaterial.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths ++= Seq(
      "angular-material" -> s"/${webModulesLib.value}/angular-material/angular-material"
    ),
    requireConfigurationShim +=
      "angular-material" -> RequireConfiguration.Shim.Config(
        Seq("angular", "angular-animate", "angular-aria"),
        init = Some(
          Javascript.Function(
            """function(angular) {
              |  return angular.module('ngMaterial');
              |}
            """.stripMargin
          )
        )
      )
  )

  override val projectSettings = Seq(
    angularMaterialVersion := "1.0.0",
    libraryDependencies += "org.webjars" % "angular-material" % angularMaterialVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)

}