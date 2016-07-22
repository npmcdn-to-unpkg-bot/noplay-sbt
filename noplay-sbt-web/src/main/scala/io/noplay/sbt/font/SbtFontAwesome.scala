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
package io.noplay.sbt.font

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import io.noplay.sbt.require.SbtRequire
import SbtRequire.autoImport._
import io.noplay.sbt.require.SbtRequire
import sbt.Keys._
import sbt._

object SbtFontAwesome
  extends AutoPlugin {
  override lazy val requires = SbtRequire

  object autoImport {
    val fontAwesomeVersion = settingKey[String]( "The font awesome version" )
  }

  import SbtFontAwesome.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "font-awesome" -> s"/${webModulesLib.value}/font-awesome"
  )

  override lazy val projectSettings = Seq(
    fontAwesomeVersion := "4.6.3",
    libraryDependencies += "org.webjars" % "font-awesome" % fontAwesomeVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}