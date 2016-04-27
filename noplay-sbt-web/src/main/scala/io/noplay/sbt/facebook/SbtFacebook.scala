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
package io.noplay.sbt.facebook

import com.typesafe.sbt.web.Import._
import io.noplay.sbt.SbtRequire
import io.noplay.sbt.SbtRequire.autoImport.RequireConfiguration.Shim
import io.noplay.sbt.SbtRequire.autoImport._
import sbt._

object SbtFacebook
  extends AutoPlugin {
  override val requires = SbtRequire

  object autoImport {
    val facebookDefaultVersion = settingKey[String]("Facebook Sdk version")
    val facebookDefaultXfbmlEnabled = settingKey[Boolean]("Facebook Sdk xfbml")
  }

  import SbtFacebook.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "facebook" -> "//connect.facebook.net/en_US/sdk",
    requireConfigurationShim ++= Seq(
      "facebook" -> Shim.Config(
        deps = Seq("module"),
        exports = Option("FB"),
        init = Some(
          Javascript.Function(
            s"""function(module) {
              |  var config = (module.config && module.config()) || {};
              |  FB.init({
              |    appId: config.appId,
              |    xfbml: config.xfbml || ${facebookDefaultXfbmlEnabled.value},
              |    version: config.version || '${facebookDefaultVersion.value}'
              |  });
              |}
            """.stripMargin
          )
        )
      )
    )
  )

  override val projectSettings = Seq(
    facebookDefaultVersion := "2.5",
    facebookDefaultXfbmlEnabled := true
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}