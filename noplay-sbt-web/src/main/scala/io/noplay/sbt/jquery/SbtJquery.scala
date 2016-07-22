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
package io.noplay.sbt.jquery

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import io.noplay.sbt.require.SbtRequire
import io.noplay.sbt.require.SbtRequire.autoImport.RequireConfiguration.Shim
import io.noplay.sbt.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtJquery
  extends AutoPlugin {
  override val requires = SbtRequire

  object autoImport {
    val jqueryVersion = settingKey[String]( "The Jquery version" )
  }

  import SbtJquery.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "jquery" -> s"/${webModulesLib.value}/jquery/dist/jquery",
    requireConfigurationShim += "jquery" -> Shim.Config( exports = Some( "$" ) )
  )

  override val projectSettings = Seq(
    jqueryVersion := "3.0.0",
    libraryDependencies += "org.webjars.npm" % "jquery" % jqueryVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}