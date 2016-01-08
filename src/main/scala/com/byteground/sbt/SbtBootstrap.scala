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
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

object SbtBootstrap
  extends AutoPlugin {
  override lazy val requires = SbtRequire

  object autoImport {
    val bootstrapVersion = settingKey[String]( "Bootstrap Js version" )
  }

  import com.byteground.sbt.SbtBootstrap.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "bootstrap" -> s"/${webModulesLib.value}/bootstrap"
  )

  override lazy val projectSettings = Seq(
    bootstrapVersion := "3.3.5",
    libraryDependencies += "org.webjars" % "bootstrap" % bootstrapVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}