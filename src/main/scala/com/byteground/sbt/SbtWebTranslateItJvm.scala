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

import sbt.Keys._
import sbt._

object SbtWebTranslateItJvm
  extends AutoPlugin {

  override lazy val requires = SbtWebTranslateIt

  object autoImport {
    val webTranslateItResources = taskKey[Seq[File]]("Add translated files to the resources.")
  }

  import autoImport._
  import SbtWebTranslateIt.autoImport._

  def configurationSettings(config: Configuration): Seq[Setting[_]] =
    SbtWebTranslateIt.configurationSettings(config) ++ inConfig(config)(
      Seq(
        webTranslateItMasterDirectories := resourceDirectories.value,
        webTranslateItResources := webTranslateItPull.value,
        managedResourceDirectories <+= webTranslateItTranslatedLocalDirectory,
        resourceGenerators <+= webTranslateItResources
      )
    )

  override lazy val projectSettings = configurationSettings(Compile) ++ configurationSettings(Test)
}