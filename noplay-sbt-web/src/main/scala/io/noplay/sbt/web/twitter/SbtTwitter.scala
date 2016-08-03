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
package io.noplay.sbt.web.twitter

import com.typesafe.sbt.web.Import._
import io.noplay.sbt.web.require.SbtRequire
import SbtRequire.autoImport.RequireConfiguration.Shim
import SbtRequire.autoImport._
import io.noplay.sbt.web.require.SbtRequire
import sbt._

object SbtTwitter
  extends AutoPlugin {

  override val requires = SbtRequire

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "twitter" -> "//platform.twitter.com",
    requireConfigurationShim ++= Seq(
      "twitter" -> Shim.Config(
        Seq(),
        exports = Some("twttr")
      )
    )
  )

  override val projectSettings =
    inConfig(Assets)(unscopedProjectSettings) ++
      inConfig(TestAssets)(unscopedProjectSettings)

}