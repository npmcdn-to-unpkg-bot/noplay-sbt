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

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt._

import scala.language.existentials

object SbtBootloader
  extends AutoPlugin {

  import com.byteground.sbt.SbtWebUtil.autoImport._
  import com.byteground.sbt.SbtWebIndex.autoImport._

  override lazy val requires = SbtWebUtil

  object autoImport {
    val bootloaderStyle = settingKey[Style]( "Bootloader style" )
  }

  import autoImport._

  val unscopedProjectSettings = Seq(
    bootloaderStyle := s"${webModulesLib.value}/${webUtilName.value}/bootloader/bootloader.css",
    webIndexEmbeddedStyles ++= Seq[Style](
      bootloaderStyle.value
    ),
    webIndexEmbeddedScripts ++= Seq[Script](
      s"${webModulesLib.value}/${webUtilName.value}/bootloader/bootloader.js"
    )
  ) ++ Seq(
    webUtilPath( "bootloader" )
  )

  override lazy val projectSettings =
    inConfig( Assets )( unscopedProjectSettings ) ++
      inConfig( TestAssets )( unscopedProjectSettings )
}