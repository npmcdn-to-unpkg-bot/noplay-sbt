/**
 * Copyright © 2009-2015 ByTeGround, Inc
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

import com.byteground.sbt.SbtRequireJs.autoImport._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

object SbtJquery
  extends AutoPlugin {
  override lazy val requires = SbtRequireJs

  object autoImport {
    val jqueryVersion = settingKey[String]( "The Jquery version" )
  }

  import com.byteground.sbt.SbtJquery.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths += "jquery" -> s"${webModulesLib.value}/jquery/jquery",
    requireJsConfigurationShim += "jquery" -> RequireJsConfiguration.Shim.Config( exports = Some( "jQuery" ) )
  )

  override lazy val projectSettings = Seq(
    jqueryVersion := "2.1.1",
    libraryDependencies += "org.webjars" % "jquery" % jqueryVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}