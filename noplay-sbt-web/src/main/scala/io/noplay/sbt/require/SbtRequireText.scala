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
package io.noplay.sbt.require

import com.typesafe.sbt.web.SbtWeb.autoImport._
import io.noplay.sbt.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtRequireText
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val requireTextVersion = settingKey[String]("Require text version")
  }

  import SbtRequireText.autoImport._

  override lazy val projectSettings = Seq(
    requireTextVersion := "2.0.15",
    libraryDependencies += "org.webjars" % "requirejs-text" % requireTextVersion.value
  ) ++ inConfig(Assets)(unscopedSettings) ++ inConfig(TestAssets)(unscopedSettings)

  private lazy val unscopedSettings = Seq(
    requireConfigurationPaths += "text" -> s"/${WebKeys.webModulesLib.value}/requirejs-text/text"
  )

}