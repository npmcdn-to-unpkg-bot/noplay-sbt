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
import sbt.plugins.JvmPlugin

object SbtWebTranslateItJvm
  extends AutoPlugin {

  override lazy val requires = SbtWebTranslateIt && JvmPlugin

  import SbtWebTranslateIt.autoImport._

  def configurationSettings(config: Configuration): Seq[Setting[_]] =
    SbtWebTranslateIt.configurationSettings(config) ++ inConfig(config)(
      Seq(
        webTranslateItMasterDirectories <<= unmanagedResourceDirectories,
        resources := {
          val masterFiles = webTranslateItMasterFiles.value.toSet
          resources.value.filterNot(masterFiles.contains)
        },
        managedResourceDirectories ++= Seq(
          webTranslateItMasterMergedDirectory.value,
          webTranslateItTranslatedLocalDirectory.value
        ),
        resourceGenerators <+= webTranslateItPull
      )
    )

  override lazy val projectSettings = configurationSettings(Compile) ++ configurationSettings(Test)
}