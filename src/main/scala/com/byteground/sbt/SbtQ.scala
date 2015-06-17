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

object SbtQ
  extends AutoPlugin {
  override lazy val requires = SbtRequireJs

  object autoImport {
    val qVersion = settingKey[String]( "The Q library version" )
  }

  import com.byteground.sbt.SbtQ.autoImport._

  val unscopedProjectSettings = Seq(
    requireJsConfigurationPaths += "q" -> s"${webModulesLib.value}/q/q"
  )

  override lazy val projectSettings = Seq(
    qVersion := "1.0.1",
    libraryDependencies += "org.webjars" % "q" % qVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}