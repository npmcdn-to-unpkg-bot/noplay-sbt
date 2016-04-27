/**
 * Copyright © 2009-2016 Hydra Technologies, Inc
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
package io.alphard.sbt

import java.net.URI
import java.nio.charset.Charset
import java.util.Locale

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.web._
import com.typesafe.sbt.web.pipeline.Pipeline._
import sbt.Keys._
import sbt._

import scala.language.implicitConversions

object SbtWebIndex
  extends AutoPlugin {

  override lazy val requires: Plugins = SbtWeb

  object autoImport {

    final case class Meta(name: String, content: String)

    object Resource {
      object Type {
        val Javascript = "text/javascript"
        val Css = "text/css"
      }
      implicit def stringSeqToResourceSeq[R <: Resource](paths: Seq[String])
                                                        (implicit stringToResource: String => R): Seq[R] =
        paths.map(stringToResource)
    }

    sealed trait Resource {
      lazy val isAbsolute: Boolean = path.startsWith("/") || (!path.startsWith("file") && path.matches("^://"))
      lazy val isRelative: Boolean = !isAbsolute
      def `type`: String
      def path: String
      def attributes: Map[String, String]
    }

    object Style {
      implicit def stringToStyle(path: String): Style = Style(path)
    }

    final case class Style(path: String,
                           `type`: String = Resource.Type.Css,
                           attributes: Map[String, String] = Map.empty[String, String]) extends Resource

    object Script {
      implicit def stringToScript(path: String): Script = Script(path)
    }

    final case class Script(path: String,
                            `type`: String = Resource.Type.Javascript,
                            async: Boolean = false,
                            attributes: Map[String, String] = Map.empty[String, String]) extends Resource

    lazy val webIndexName = settingKey[String]("The index file name")
    lazy val webIndexDirectory = settingKey[File]("The index file directory")
    lazy val webIndexFile = settingKey[File]("The index file")
    lazy val webIndexAsync = settingKey[Boolean]("Handle styles and scripts asynchronously")
    lazy val webIndexAsyncDependencies = settingKey[Option[String]]("The async dependencies javascript array name.")
    lazy val webIndexLocale = settingKey[Locale]("The html file locale")
    lazy val webIndexCharset = settingKey[Charset]("The charset meta")
    lazy val webIndexTitle = settingKey[String]("The index title")
    lazy val webIndexDescription = settingKey[String]("The index meta description")
    lazy val webIndexMetas = settingKey[Seq[Meta]]("The extra meta tags")
    lazy val webIndexEmbeddedStyles = settingKey[Seq[Style]]("The styles to be embedded in the index page")
    lazy val webIndexEmbeddedScripts = settingKey[Seq[Script]]("The scripts to be embedded in the index page")
    lazy val webIndexStyles = settingKey[Seq[Style]]("The styles to be downloaded from the index page")
    lazy val webIndexScripts = settingKey[Seq[Script]]("The scripts to be downloaded from the page")
    lazy val webIndexHtmlAttributes = settingKey[Seq[(String, String)]]("The extra html tag attributes")
    lazy val webIndexHeadFile = settingKey[File]("The extra head file tag elements")
    lazy val webIndexHead = settingKey[String]("The extra head tag elements")
    lazy val webIndexHeadAttributes = settingKey[Seq[(String, String)]]("The extra head tag attributes")
    lazy val webIndexBodyFile = settingKey[File]("The body file. If present it is used to generate the body content")
    lazy val webIndexBody = settingKey[String]("The body content")
    lazy val webIndexBodyAttributes = settingKey[Seq[(String, String)]]("The extra body attributes")
    lazy val webIndexStage = taskKey[Stage]("The pipeline stage that generates the index page")
  }

  import SbtWebIndex.autoImport._

  lazy val unscopedProjectSettings: Seq[Setting[_]] = Seq(
    webIndexName := "index.html",
    webIndexDirectory := target.value / "web-index",
    webIndexFile := webIndexDirectory.value / webIndexName.value,
    webIndexAsync := false,
    webIndexAsyncDependencies := Some("$dependencies"),
    webIndexHtmlAttributes := Nil,
    webIndexLocale := Locale.getDefault,
    webIndexCharset := Charset.forName("UTF-8"),
    webIndexTitle := name.value,
    webIndexDescription := description.value,
    webIndexMetas := Seq(
      Meta("viewport", "width=device-width, initial-scale=1")
    ),
    webIndexEmbeddedStyles := Nil,
    webIndexEmbeddedScripts := Nil,
    webIndexStyles := Nil,
    webIndexScripts := Nil,
    webIndexHeadFile := sourceDirectory.value / "head.html",
    webIndexHead := {
      val file = webIndexHeadFile.value
      if (file.exists())
        IO.read(file, webIndexCharset.value)
      else
        ""
    },
    webIndexHeadAttributes := Nil,
    webIndexBodyFile := sourceDirectory.value / "body.html",
    webIndexBody := {
      val file = webIndexBodyFile.value
      if (file.exists())
        IO.read(file, webIndexCharset.value)
      else
        "    <div>It works!</div>"
    },
    webIndexBodyAttributes := Nil,
    excludeFilter := excludeFilter.value || new SimpleFileFilter(f => f == webIndexBodyFile.value || f == webIndexHeadFile.value),
    webIndexStage := Def.task {
      (mappings: Seq[PathMapping]) =>
        implicit val logger = streams.value.log
        val async = webIndexAsync.value

        val resolvedEmbeddedStyles = resolveResources(mappings, webIndexEmbeddedStyles.value)
        val resolvedEmbeddedScripts = resolveResources(mappings, webIndexEmbeddedScripts.value)

        val content =
          s"""<!DOCTYPE html>
              |<html lang="${webIndexLocale.value.getLanguage}"${generateAttributes(webIndexHtmlAttributes.value)}>
              |<head${generateAttributes(webIndexHeadAttributes.value)}>
              |    <meta charset="${webIndexCharset.value.name}">
              |    <meta http-equiv="X-UA-Compatible" content="IE=edge">
              |    <title>${webIndexTitle.value}</title>
              |    <meta name="description" content="${webIndexDescription.value}">
              |${webIndexMetas.value.map(m => "    <meta name=\"%s\" content=\"%s\">".format(m.name, m.content)).mkString("\n")}
              |${embeddedResources("style", resolvedEmbeddedStyles, webIndexCharset.value)}
              |${if (!async) downloadedResources("    <link rel=\"stylesheet\" type=\"%s\" href=\"%s\"%s/>", webIndexStyles.value) else ""}
              |${webIndexHead.value}
              |</head>
              |<body${generateAttributes(webIndexBodyAttributes.value)}>
              |${webIndexBody.value}
              |${embeddedResources("script", resolvedEmbeddedScripts, webIndexCharset.value)}
              |${if (async) asyncDependencies(webIndexAsyncDependencies.value, webIndexStyles.value, webIndexScripts.value) else ""}
              |${if (!async) downloadScripts(webIndexScripts.value) else ""}
              |</body>
              |</html>
              |""".stripMargin

        IO.write(webIndexFile.value, content, webIndexCharset.value, append = false)

        val resolvedEmbeddedLocalFiles = (resolvedEmbeddedStyles ++ resolvedEmbeddedScripts) collect {
          case (uri, resource) if resource.isRelative => new File(uri.toURL.getFile)
        }
        val webIndexPath = IO.relativize(webIndexDirectory.value, webIndexFile.value).getOrElse(webIndexName.value)
        (mappings :+(webIndexFile.value, webIndexPath)) filterNot {
          case (file, path) =>
            resolvedEmbeddedLocalFiles.contains(file)
        }
    }.dependsOn(webModules, nodeModules).value,
    pipelineStages ++= Seq(webIndexStage)
  )

  override lazy val projectSettings =
    inConfig(Assets)(unscopedProjectSettings) ++
      inConfig(TestAssets)(unscopedProjectSettings)

  private def generateAttributes(attributes: Seq[(String, String)]): String =
    attributes.map(a => " %s=\"%s\"".format(a._1, a._2)).mkString("")

  private def asyncDependencies(variableOption: Option[String], styles: Seq[Style], scripts: Seq[Script])
                               (implicit logger: Logger): String = {
    variableOption map {
      variable =>
        s"""<script type="text/javascript">
            |  var $variable = {
            |    "styles": [
            |      ${styles.map(s => "\"" + s.path + "\"").mkString(",\n      ")}
            |    ],
            |    "scripts": [
            |      ${scripts.map(s => "\"" + s.path + "\"").mkString(",\n      ")}
            |    ]
            |  }
            |</script>""".stripMargin
    } getOrElse ""
  }

  private def embeddedResources[R <: Resource](tag: String, resources: Seq[(URI, R)], charset: Charset)
                                              (implicit logger: Logger): String =
    ("" /: resources.groupBy(_._2.`type`)) {
      case (content, (tpe, typedStyles)) =>
        content + ("" /: typedStyles) {
          case (typedContent, (uri, resource)) =>
            typedContent +
              s"""<$tag type="$tpe"${generateAttributes(resource.attributes.toSeq)}>
                  |/* from ${resource.path} */
                  |${IO.readStream(uri.toURL.openStream(), charset)}
                  |</$tag>
                  |""".stripMargin
        }
    }

  private def downloadScripts(scripts: Seq[Script]): String =
    scripts.groupBy(_.async) map {
      case (async, groupedScripts) if async =>
        downloadedResources("    <script async type=\"%s\" src=\"%s\"%s></script>", groupedScripts)
      case (async, groupedScripts) =>
        downloadedResources("    <script type=\"%s\" src=\"%s\"%s></script>", groupedScripts)
    } mkString "\n"

  private def downloadedResources[R <: Resource](pattern: String, resources: Seq[R]): String =
    resources map {
      case resource =>
        pattern.format(resource.`type`, resource.path, generateAttributes(resource.attributes.toSeq))
    } mkString "\n"

  private def resolveResources[R <: Resource](mappings: Seq[PathMapping], resources: Seq[R])
                                             (implicit logger: Logger): Seq[(URI, R)] = {
    val (absoluteResources, relativeResources) = resources.partition(_.isAbsolute)
    absoluteResources.map(r => (URI.create(r.path), r)) ++ (relativeResources flatMap {
      case resource =>
        mappings find {
          case (file, path) =>
            resource.path == path.replace('\\', '/')
        } map {
          case (file, path) =>
            (file.toURI, resource)
        } orElse {
          logger.warn(s"mapping not found for path: ${resource.path}!")
          Option.empty[(URI, R)]
        }
    })
  }
}