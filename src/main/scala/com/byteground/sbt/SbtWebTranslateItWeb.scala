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

import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web.pipeline.Pipeline.Stage
import com.typesafe.sbt.web.{PathMapping, SbtWeb}
import sbt.Keys._
import sbt._
import scala.collection.breakOut

object SbtWebTranslateItWeb
  extends AutoPlugin {

  override lazy val requires = SbtWebTranslateIt && SbtWeb

  object autoImport {
    val webTranslateItStage = taskKey[Stage]("Add translated files to the mappings.")
  }

  import autoImport._
  import SbtWebTranslateIt.autoImport._

  def configurationSettings(config: Configuration): Seq[Setting[_]] =
    SbtWebTranslateIt.configurationSettings(config) ++ inConfig(config)(
      Seq(
        webTranslateItMasterDirectories <<= sourceDirectories,
        webTranslateItStage := {
          (mappings: Seq[PathMapping]) =>
            val pulledFiles = webTranslateItPull.value
            val mergedDir = webTranslateItMasterMergedDirectory.value
            val translDir = webTranslateItTranslatedLocalDirectory.value
            val localMasterPathes: Set[String] = webTranslateItMasterFiles.value.map(_.getAbsolutePath)(breakOut)
            mappings.filterNot {
              case (localMasterFile, _) =>
                localMasterPathes.contains(localMasterFile.getAbsolutePath)
            } ++ util.IO.mapAllFiles(Seq(mergedDir, translDir), pulledFiles)
        },
        managedSourceDirectories <+= webTranslateItTranslatedLocalDirectory,
        pipelineStages += webTranslateItStage
      )
    )

  override lazy val projectSettings = configurationSettings(Assets) ++ configurationSettings(TestAssets)
}