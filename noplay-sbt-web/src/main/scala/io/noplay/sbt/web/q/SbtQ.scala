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
package io.noplay.sbt.web.q

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import io.noplay.sbt.web.require.SbtRequire
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtQ
  extends AutoPlugin {
  override val requires = SbtRequire

  object autoImport {
    val qVersion = settingKey[String]("The Q library version")
  }

  import SbtQ.autoImport._

  override val projectSettings = Seq(
    qVersion := "1.4.1",
    libraryDependencies += "org.webjars.npm" % "q" % qVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)

  private lazy val unscopedProjectSettings = Seq(
    requireMainConfigPaths += "q" -> s"/${webModulesLib.value}/q/q",
    //requireMainConfigMinifiedPaths += "q" -> s"/${webModulesLib.value}/q/q",
    requireMainConfigCDNPaths += "q" -> s"//cdnjs.cloudflare.com/ajax/libs/q.js/${qVersion.value}/q.min",
    requireMainConfigShim += "q" -> RequireShimConfig(exports = Some("Q"))
  )
}