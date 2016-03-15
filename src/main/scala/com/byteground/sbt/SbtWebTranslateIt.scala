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

import com.byteground.sbt.webtranslateit.WebTranslateItClient
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.util.{DefaultIndenter, DefaultPrettyPrinter}
import com.fasterxml.jackson.databind.ObjectWriter
import com.typesafe.sbt.web.Import._
import java.net.URI
import org.json4s._
import org.json4s.jackson.JsonMethods
import org.json4s.jackson._
import sbt.Keys._
import sbt._
import scala.collection.breakOut
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

object SbtWebTranslateIt
  extends AutoPlugin {

  trait MergeStrategy extends ((File, File, File) => Boolean) {
    final def apply(local: File, remote: File, out: File): Boolean = {
      (local.exists(), remote.exists()) match {
        case (true, true) =>
          merge(local, remote, out)
        case (true, false) =>
          IO.copyFile(local, out)
          false
        case (false, true) =>
          IO.copyFile(remote, out)
          false
        case _ =>
          throw new IllegalArgumentException(s"cannot merge nonexistent files: $local <=> $remote")
      }
    }

    protected def merge(local: File, remote: File, out: File): Boolean
  }

  object JsonMergeStrategy extends MergeStrategy {
    protected def merge(local: File, remote: File, out: File): Boolean = {
      val localJson = JsonMethods.parse(IO.read(local, IO.utf8)) match {
        case jobj: JObject => jobj
        case _ => throw new IllegalArgumentException(s"incorrect format for file: $local")
      }
      val remoteJson = JsonMethods.parse(IO.read(remote, IO.utf8)) match {
        case jobj: JObject => jobj
        case _ => throw new IllegalArgumentException(s"incorrect format for file: $remote")
      }

      var merged = false

      //val keys = (localJson.obj.map(_._1) ++ remoteJson.obj.map(_._1)).distinct
      val keys = localJson.obj.map(_._1)
      val mergedJson = JObject(
        keys.map { key =>
          key -> (remoteJson \ key match {
            case JNull|JNothing =>
              merged = true
              localJson \ key
            case jvalue =>
              val diff = jvalue.diff(localJson \ key)
              merged = diff.added != JNothing || diff.changed != JNothing || diff.deleted != JNothing
              jvalue
          })
        }
      )

      IO.write(out, jsonWriter.writeValueAsString(mergedJson), IO.utf8)
      merged
    }

    private val jsonWriter = JsonMethods.mapper.writer[ObjectWriter](new PrettyPrinter)
    private class PrettyPrinter extends DefaultPrettyPrinter {
      final override def createInstance(): PrettyPrinter = new PrettyPrinter
      final override def writeObjectFieldValueSeparator(jg: JsonGenerator) { jg.writeRaw(": ") }
      _objectIndenter = new DefaultIndenter("  ", "\n")
    }
  }

  final case class SyncResult(
    deletedRemote: Seq[String],
    updatedRemote: Seq[String],
    updatedLocal: Seq[File]
  )

  object autoImport {
    val WebTranslateItRealm = "WebTranslateIt Realm"

    val webTranslateItRealm = settingKey[String]("The WebTranslateIt realm")

    val webTranslateItApiUri = settingKey[URI]("The WebTranslateIt API uri")
    val webTranslateItProjectToken = taskKey[String]("The WebTranslateIt project token")

    val webTranslateItClient = taskKey[WebTranslateItClient]("The WebTranslateIt REST client")
    val webTranslateItProject = taskKey[WebTranslateItClient.Project]("Show project.")

    val webTranslateItTargetDirectory = settingKey[File]("WebTranslateIt working directory")

    val webTranslateItMasterDirectories = settingKey[Seq[File]]("Master local directories")
    val webTranslateItMasterIncludeFilter = settingKey[FileFilter]("File filter to include master local files")
    val webTranslateItMasterExcludeFilter = settingKey[FileFilter]("File filter to exclude master local files")
    val webTranslateItMasterFiles = taskKey[Seq[File]]("Retrieve master local files.")

    val webTranslateItMasterRemoteDirectory = settingKey[File]("Master remote files directory")
    val webTranslateItMasterRemoteFiles = taskKey[Seq[File]]("Pull master remote files from the WebTranslateIt repository.")

    val webTranslateItMasterFilesMergeStrategy = settingKey[MergeStrategy]("Strategy for merging master local and remote files")
    val webTranslateItMasterMergedDirectory = settingKey[File]("Master merged files directory")
    val webTranslateItMasterMergedFiles = taskKey[Seq[File]]("Generate master merged files.")

    val webTranslateItTranslatedRemoteDirectory = settingKey[File]("Translated remote files directory")
    val webTranslateItTranslatedRemoteFiles = taskKey[Seq[File]]("Pull translated remote files from the WebTranslateIt repository.")

    val webTranslateItTranslatedLocalDirectory = settingKey[File]("Translated local files directory")
    val webTranslateItTranslatedLocalFiles = taskKey[Seq[File]]("Translated local files")

    val webTranslateItPull = taskKey[Seq[File]]("Pull translation files from the WebTranslateIt repository.")
    val webTranslateItSync = taskKey[SyncResult]("Sync local and remote master files according to the WebTranslateIt repository.")
  }

  import autoImport._
  import ExecutionContext.Implicits.global

  def configurationSettings(config: Configuration): Seq[Setting[_]] = inConfig(config)(
    Seq(
      webTranslateItTargetDirectory := target.value / "webtranslateit" / config.name,
      webTranslateItMasterIncludeFilter := new SimpleFilter(_.endsWith("en_US.json")),
      webTranslateItMasterExcludeFilter := NothingFilter,
      webTranslateItMasterRemoteDirectory := webTranslateItTargetDirectory.value / "remote" / "master",
      webTranslateItMasterRemoteFiles := {
        val log = streams.value.log
        val client = webTranslateItClient.value
        val dir = webTranslateItMasterRemoteDirectory.value
        val project = webTranslateItProject.value
        val files: Seq[File] = Await.result(
          Future.sequence(
            project.project_files.filter(_.master_project_file_id.isEmpty).map {
              projectFile =>
                client.showFile(projectFile.id, project.source_locale.code) map {
                  content =>
                    val file = dir / projectFile.name
                    IO.write(file, content, IO.utf8, append = false)
                    log.debug(s"SbtWebTranslateIt - downloaded project_file=${projectFile.name} from ${webTranslateItApiUri.value}")
                    file
                }
            }
          ),
          Duration.Inf
        )
        files
      },
      webTranslateItMasterMergedDirectory := webTranslateItTargetDirectory.value / "merged" / "master",
      webTranslateItMasterFilesMergeStrategy := JsonMergeStrategy,
      webTranslateItTranslatedRemoteDirectory := webTranslateItTargetDirectory.value / "remote" / "translated",
      webTranslateItTranslatedRemoteFiles := {
        val log = streams.value.log
        val client = webTranslateItClient.value
        val dir = webTranslateItTranslatedRemoteDirectory.value
        val project = webTranslateItProject.value
        val files: Seq[File] = Await.result(
          Future.sequence(
            project.project_files.filter(_.master_project_file_id.nonEmpty).map {
              projectFile =>
                client.showFile(projectFile.id, projectFile.locale_code) map {
                  content =>
                    val file = dir / projectFile.name
                    IO.write(file, content, IO.utf8, append = false)
                    log.debug(s"SbtWebTranslateIt - downloaded project_file=${projectFile.name} from ${webTranslateItApiUri.value}")
                    file
                }
            }
          ),
          Duration.Inf
        )
        files
      },
      webTranslateItMasterDirectories := Nil,
      webTranslateItMasterFiles := {
        val filter = webTranslateItMasterIncludeFilter.value -- webTranslateItMasterExcludeFilter.value
        webTranslateItMasterDirectories.value.flatMap(util.IO.listAllFiles(_, filter)).get
      },
      webTranslateItMasterMergedFiles := {
        val log = streams.value.log
        val mergeStrategy = webTranslateItMasterFilesMergeStrategy.value
        val remoteDir = webTranslateItMasterRemoteDirectory.value
        val mergedDir = webTranslateItMasterMergedDirectory.value
        val sourceDirs = webTranslateItMasterDirectories.value :+ remoteDir
        val localFiles = mapAllFiles(sourceDirs, webTranslateItMasterFiles.value)
        val remoteFiles = mapAllFiles(sourceDirs, webTranslateItMasterRemoteFiles.value)
        val mergedFiles: Seq[File] =
          (localFiles.keySet ++ remoteFiles.keySet).flatMap {
            key =>
              val mergedFile = mergedDir / key
              (localFiles.get(key), remoteFiles.get(key)) match {
                case (Some(localFile), Some(remoteFile)) =>
                  val merged = mergeStrategy(localFile, remoteFile, mergedFile)
                  if (merged) {
                    log.info(s"SbtWebTranslateIt - merged project_file=$key from ${webTranslateItApiUri.value}")
                  }
                  Some(mergedFile)
                case (Some(localFile), None) =>
                  IO.copyFile(localFile, mergedFile)
                  Some(mergedFile)
                case _ => None
              }
          }(breakOut)
        mergedFiles
      },
      webTranslateItTranslatedLocalDirectory := webTranslateItTargetDirectory.value / "local",
      webTranslateItTranslatedLocalFiles := {
        val log = streams.value.log
        val localDir = webTranslateItTranslatedLocalDirectory.value
        val remoteDir = webTranslateItTranslatedRemoteDirectory.value
        val remoteFiles = mapAllFiles(Seq(remoteDir), webTranslateItTranslatedRemoteFiles.value)
        val files: Seq[File] = remoteFiles.map { case (key, remoteFile) =>
          val localFile = localDir / key
          IO.copyFile(remoteFile, localFile)
          log.info(s"SbtWebTranslateIt - downloaded project_file=$key from ${webTranslateItApiUri.value}")
          localFile
        }(breakOut)
        files
      },
      webTranslateItPull := {
        webTranslateItMasterMergedFiles.value ++
          webTranslateItTranslatedLocalFiles.value
      },
      webTranslateItSync := {
        val log = streams.value.log
        val client = webTranslateItClient.value
        val mergedDir = webTranslateItMasterMergedDirectory.value
        val mergedFiles = mapAllFiles(Seq(mergedDir), webTranslateItMasterMergedFiles.value)
          .filter { case (key, mergedFile) =>
            if (mergedFile.length == 0L) {
              log.warn(s"SbtWebTranslateIt - empty project_file=$key")
              false
            } else true
          }
        val project = webTranslateItProject.value
        val projectFiles = project.project_files.filter(_.master_project_file_id.isEmpty)
        val projectFileKeys = projectFiles.map(_.name).toSet

        val deletedRemote: Seq[String] = Await.result(
          Future.sequence(
            projectFiles
              .filterNot { projectFile =>
                mergedFiles.contains(projectFile.name)
              }
              .map { projectFile =>
                client.deleteFile(projectFile.id) map { _ =>
                  log.info(s"SbtWebTranslateIt - deleted project_file=${projectFile.name} on ${webTranslateItApiUri.value}")
                  projectFile.name
                }
              }
          ),
          Duration.Inf
        )

        val updatedRemote: Seq[String] = Await.result(
          Future.sequence(
            mergedFiles.toSeq.map { case (key, mergedFile) =>
              val is = new java.io.FileInputStream(mergedFile)
              (if (projectFileKeys.contains(key)) {
                client.updateFile(key, is) map { _ =>
                  log.info(s"SbtWebTranslateIt - updated project_file=$key on ${webTranslateItApiUri.value}")
                  key
                }
              } else {
                client.createFile(key, is) map { _ =>
                  log.info(s"SbtWebTranslateIt - created project_file=$key on ${webTranslateItApiUri.value}")
                  key
                }
              }) andThen { case _ => is.close() }
            }
          ),
          Duration.Inf
        )

        val sourceDirs = webTranslateItMasterDirectories.value
        val sourceFiles = mapAllFiles(sourceDirs, webTranslateItMasterFiles.value)

        val updatedLocal: Seq[File] = mergedFiles.keySet.flatMap {
          key =>
            val mergedFile = mergedFiles(key)
            sourceFiles.get(key).map { sourceFile =>
              IO.copyFile(mergedFile, sourceFile)
              sourceFile
            }
        }(breakOut)

        SyncResult(deletedRemote, updatedRemote, updatedLocal)
      }
    )
  )

  override lazy val projectSettings = Seq(
    webTranslateItRealm := WebTranslateItRealm,
    webTranslateItApiUri := URI.create("https://webtranslateit.com/api/"),
    webTranslateItProjectToken := {
      credentials.value
        .map(Credentials.toDirect)
        .find(_.realm == webTranslateItRealm.value)
        .fold(sys.error(s"no project token provided for WebTranslateIt realm: ${webTranslateItRealm.value}!"))(_.passwd)
    },
    webTranslateItClient := {
      WebTranslateItClient(
        webTranslateItApiUri.value,
        webTranslateItProjectToken.value
      )
    },
    webTranslateItProject := {
      Await.result(webTranslateItClient.value.showProject(), Duration.Inf)
    }
  )

  private def normalize(files: Traversable[(File, String)]): Traversable[(File, String)] = files.map { t2 => t2.copy(_2 = t2._2.replace('\\', '/')) }
  private def mapAllFiles(bases: Seq[File], files: Seq[File], filter: FileFilter = AllPassFilter): Map[String, File] = normalize(util.IO.mapAllFiles(bases, files, filter)).map(_.swap)(breakOut)
}
