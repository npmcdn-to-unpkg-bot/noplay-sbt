/**
 * Copyright © 2009-2014 ByTeGround, Inc
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

object SbtAngularJs
  extends AutoPlugin {
  override lazy val requires = SbtRequireJs

  object autoImport {
    val angularJsVersion = settingKey[String]( "Angular Js version" )
  }

  import com.byteground.sbt.SbtAngularJs.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths += "angular" -> s"${webModulesLib.value}/angularjs/angular",
    requireJsConfigurationShim += "angular" -> RequireJsConfiguration.Shim.Config( exports = Some( "angular" ) )
  )

  override lazy val projectSettings = Seq(
    angularJsVersion := "1.3.4-1",
    libraryDependencies += "org.webjars" % "angularjs" % angularJsVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}