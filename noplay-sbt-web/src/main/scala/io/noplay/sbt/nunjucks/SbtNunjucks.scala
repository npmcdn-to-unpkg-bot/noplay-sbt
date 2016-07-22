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
package io.noplay.sbt.nunjucks

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import io.alphard.sbt.util.Javascript
import io.noplay.sbt.require.SbtRequire
import SbtRequire.autoImport._
import io.noplay.sbt.require.SbtRequire
import sbt.Keys._
import sbt._


object SbtNunjucks
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val nunjucksVersion = settingKey[String]("Nunjucks version")
  }

  import SbtNunjucks.autoImport._

  override lazy val projectSettings = Seq(
    nunjucksVersion := "2.4.2",
    libraryDependencies += "org.webjars" % "nunjucks" % nunjucksVersion.value
  ) ++ inConfig(Assets)(unscopedSettings) ++ inConfig(TestAssets)(unscopedSettings)

  private lazy val unscopedSettings = Seq(
    requireConfigurationPaths += "nunjucks" -> s"/${webModulesLib.value}/nunjucks",
    requireConfigurationShim += "nunjucks" -> RequireConfiguration.Shim.Config(
      init = Some(
        Javascript.Function(
          s"""function(angular) {
              |  return nunjucks;
              |}
          """.stripMargin
        )
      )
    )
  )

}