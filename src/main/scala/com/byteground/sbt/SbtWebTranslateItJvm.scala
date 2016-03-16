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
import scala.collection.breakOut

object SbtWebTranslateItJvm
  extends AutoPlugin {

  override lazy val requires = SbtWebTranslateIt && JvmPlugin

  import SbtWebTranslateIt.autoImport._

  def configurationSettings(config: Configuration): Seq[Setting[_]] =
    SbtWebTranslateIt.configurationSettings(config) ++ inConfig(config)(
      Seq(
        webTranslateItMasterDirectories := resourceDirectories.value,
        managedResourceDirectories ++= Seq(
          webTranslateItMasterMergedDirectory.value,
          webTranslateItTranslatedLocalDirectory.value),
        /*(mappings in packageBin) := {
          val pulledFiles = webTranslateItPull.value
          val mergedDir = webTranslateItMasterMergedDirectory.value
          val translDir = webTranslateItTranslatedLocalDirectory.value
          val localMasterPathes: Set[String] = webTranslateItMasterFiles.value.map(_.getAbsolutePath)(breakOut)
          streams.value.log.warn(s"mappings=${mappings in packageBin}")
          (mappings in packageBin).value.filterNot {
            case (localMasterFile, _) =>
              localMasterPathes.contains(localMasterFile.getAbsolutePath)
          } ++ util.IO.mapAllFiles(Seq(mergedDir, translDir), pulledFiles)
        },*/
        mappings in packageBin <<= (
          webTranslateItPull, webTranslateItMasterMergedDirectory,
          webTranslateItTranslatedLocalDirectory, webTranslateItMasterFiles,
          mappings in packageBin, streams
        ) map { (pulledFiles, mergedDir, translDir, masterFiles, mappings, s) =>
          val localMasterPathes: Set[String] = masterFiles.map(_.getAbsolutePath)(breakOut)
          s.log.warn(s"mappings=$mappings")
          mappings.filterNot {
            case (localMasterFile, _) =>
              localMasterPathes.contains(localMasterFile.getAbsolutePath)
          } ++ util.IO.mapAllFiles(Seq(mergedDir, translDir), pulledFiles)
        },
        resourceGenerators <+= webTranslateItPull
      )
    )

  override lazy val projectSettings = Seq(
    copyResources in packageBin <<= (
      classDirectory, resources, resourceDirectories,
      webTranslateItPull, webTranslateItMasterMergedDirectory,
      webTranslateItTranslatedLocalDirectory, webTranslateItMasterFiles,
      streams
      ) map { (
                target, resrcs, dirs,
                pulledFiles, mergedDir, translDir, masterFiles, s
              ) =>
      val cacheFile = s.cacheDirectory / "copy-resources"
      val mappings = (resrcs --- dirs) pair (rebase(dirs, target) | flat(target))

      val localMasterPathes: Set[String] = masterFiles.map(_.getAbsolutePath)(breakOut)
      val fixedMappings = mappings.filterNot {
        case (localMasterFile, _) =>
          localMasterPathes.contains(localMasterFile.getAbsolutePath)
      }
      s.log.debug("Copy resource mappings: " + fixedMappings.mkString("\n\t", "\n\t", ""))
      Sync(cacheFile)(fixedMappings)
      fixedMappings
    }
  ) ++ configurationSettings(Compile) ++
    configurationSettings(Test)
}