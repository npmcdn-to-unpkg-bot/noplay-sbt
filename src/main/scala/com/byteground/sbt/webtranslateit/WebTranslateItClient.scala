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
package com.byteground.sbt.webtranslateit

import _root_.sprayfix.http.BodyPart
import _root_.sprayfix.http.MultipartFormData
import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import akka.util.{ByteString, Timeout}
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import java.io.InputStream
import java.net.URI
import java.util.Date
import org.joda.time.format.ISODateTimeFormat
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.io.{Codec, Source}
import scala.language.implicitConversions
import scala.util.control.NonFatal
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.UnsuccessfulResponseException
import spray.httpx.marshalling.Marshaller
import spray.httpx.unmarshalling._
import spray.json._
import spray.util._

final class WebTranslateItClientException(
                                           code: Int,
                                           msg: String,
                                           error: Option[WebTranslateItClient.Error]
                                         ) extends Exception(error.fold(s"$code $msg")(e => s"$code $msg - ${e.error}"))

object WebTranslateItClient {

  private trait BasicStringUnmarshaller {
    implicit val StringUnmarshaller = new Unmarshaller[String] {
      def apply(entity: HttpEntity) = Right(entity.asString(HttpCharsets.`UTF-8`))
    }
  }

  private object BasicUnmarshallers extends BasicStringUnmarshaller

  private trait JsonSupport {

    implicit def sprayJsonUnmarshallerConverter[T](reader: RootJsonReader[T]) =
      sprayJsonUnmarshaller(reader)
    implicit def sprayJsonUnmarshaller[T: RootJsonReader] =
      Unmarshaller[T](MediaTypes.`application/json`) {
        case x: HttpEntity.NonEmpty ⇒
          val json = JsonParser(x.asString(defaultCharset = HttpCharsets.`UTF-8`))
          jsonReader[T].read(json)
      }
    implicit def sprayJsonMarshallerConverter[T](writer: RootJsonWriter[T])(implicit printer: JsonPrinter = PrettyPrinter) =
      sprayJsonMarshaller[T](writer, printer)
    implicit def sprayJsonMarshaller[T](implicit writer: RootJsonWriter[T], printer: JsonPrinter = PrettyPrinter) =
      Marshaller.delegate[T, String](ContentTypes.`application/json`) { value ⇒
        val json = writer.write(value)
        printer(json)
      }
  }

  private trait JsonProtocol
    extends DefaultJsonProtocol {

    implicit final val dateFormat = new JsonFormat[Date] {
      private val parserISO = ISODateTimeFormat.dateTimeNoMillis()
      def write(obj: Date) = JsString(parserISO.print(new org.joda.time.DateTime(obj.getTime)))
      def read(json: JsValue) = json match {
        case JsString(s) => new Date(parserISO.parseDateTime(s).getMillis)
        case jsvalue => throw new DeserializationException(s"invalid java.util.Date format: $jsvalue")
      }
    }

    implicit final val uriFormat = new JsonFormat[URI] {
      def write(obj: URI): JsValue = JsString(obj.toString)
      def read(json: JsValue): URI = json match {
        case JsString(s) => new URI(s)
        case jsvalue => throw new DeserializationException(s"invalid java.net.URI format: $jsvalue")
      }
    }

    implicit final val localeFormat = jsonFormat2(Locale)

    implicit final val projectFileFormat = jsonFormat7(ProjectFile)

    implicit final val projectFormat = jsonFormat7(Project)

    implicit final val showProjectResponseFormat = jsonFormat1(ShowProjectResponse)

    implicit final val errorFormat = jsonFormat2(Error)
  }

  private object JsonProtocol extends JsonProtocol with JsonSupport

  private implicit val actorSystem: ActorSystem = {
    val cl = getClass.getClassLoader
    val config = ConfigFactory.load(cl)
      .withValue("spray.can.client.parsing.illegal-header-warnings", ConfigValueFactory.fromAnyRef("off"))
    ActorSystem("web-translate-it", config, cl)
  }

  final case class Locale(
                           name: String,
                           code: String
                         )

  final case class ProjectFile(
                                id: Long,
                                name: String,
                                created_at: Date,
                                updated_at: Date,
                                hash_file: String,
                                master_project_file_id: Option[Long],
                                locale_code: String
                              )

  final case class Project(
                            id: Long,
                            name: String,
                            created_at: Date,
                            updated_at: Date,
                            source_locale: Locale,
                            target_locales: Seq[Locale],
                            project_files: Seq[ProjectFile]
                          )

  final case class ShowProjectResponse(project: Project)

  final case class Error(
                          error: String,
                          request: URI
                        )
}

final case class WebTranslateItClient(
                                       apiUri: URI,
                                       projectToken: String,
                                       timeout: FiniteDuration = 60.seconds
                                     )(implicit ec: ExecutionContext) {

  import WebTranslateItClient.BasicUnmarshallers._
  import WebTranslateItClient.JsonProtocol._
  import WebTranslateItClient.actorSystem

  // Project API
  def showProject(): Future[WebTranslateItClient.Project] =
    executeQuery[WebTranslateItClient.ShowProjectResponse](
      uri + s"projects/$projectToken.json",
      HttpMethods.GET
    ) map (_.project)

  // File API
  def showFile(fileId: Long, localeCode: String): Future[String] =
    executeQuery[String](
      uri + s"projects/$projectToken/files/$fileId/locales/$localeCode",
      HttpMethods.GET
    )

  def showFile(filePath: String): Future[String] =
    executeQuery[String](
      uri + s"projects/$projectToken/files/...?file_path=$filePath",
      HttpMethods.GET
    )

  def createFile(filePath: String, content: InputStream): Future[Unit] = {
    import sprayfix.http._
    import sprayfix.httpx.marshalling.MultipartMarshallers._
    executeQuery[MultipartFormData, HttpResponse](
      uri + s"projects/$projectToken/files",
      HttpMethods.POST,
      MultipartFormData(Seq(
        BodyPart(
          HttpEntity(
            ContentTypes.`application/octet-stream`,
            HttpData(ByteString.apply(Source.fromInputStream(content)(Codec.UTF8).mkString))
          ),
          "file",
          Map.empty.updated("filename", new java.io.File(filePath).getName)
        ),
        BodyPart(HttpEntity(ContentTypes.`text/plain`, filePath), "name")
      ))
    ) map { _ => }
  }

  def updateFile(filePath: String, content: InputStream, merge: Boolean = false, ignoreMissing: Boolean = false, minorChanges: Boolean = false, label: Option[String] = None): Future[Unit] = {
    import sprayfix.http._
    import sprayfix.httpx.marshalling.MultipartMarshallers._
    executeQuery[MultipartFormData, HttpResponse](
      uri + s"projects/$projectToken/files/...?file_path=$filePath",
      HttpMethods.PUT,
      MultipartFormData(Seq(
        BodyPart(
          HttpEntity(
            ContentTypes.`application/octet-stream`,
            HttpData(ByteString.apply(Source.fromInputStream(content)(Codec.UTF8).mkString))
          ),
          "file",
          Map.empty.updated("filename", filePath)
        ),
        BodyPart(HttpEntity(ContentTypes.`text/plain`, merge.toString), "merge"),
        BodyPart(HttpEntity(ContentTypes.`text/plain`, ignoreMissing.toString), "ignore_missing"),
        BodyPart(HttpEntity(ContentTypes.`text/plain`, minorChanges.toString), "minor_changes")
      ) ++ label.map { label =>
        BodyPart(HttpEntity(ContentTypes.`text/plain`, label), "label")
      })
    ) map { _ => }
  }

  def deleteFile(fileId: Long): Future[Unit] =
    executeQuery[HttpResponse](
      uri + s"projects/$projectToken/files/$fileId",
      HttpMethods.DELETE
    ) map { _ => }

  // Locale API
  def addLocale(localeCode: String): Future[Unit] =
    executeQuery[FormData, HttpResponse](
      uri + s"projects/$projectToken/locales",
      HttpMethods.POST,
      FormData(Seq("id" -> localeCode))
    ) map { _ => }

  def deleteLocale(localeCode: String): Future[Unit] =
    executeQuery[HttpResponse](
      uri + s"projects/$projectToken/locales/$localeCode",
      HttpMethods.DELETE
    ) map { _ => }

  // Client shutdown
  def shutdown() {
    IO(Http).ask(Http.CloseAll)(1.second).await
  }

  private val uri = Uri(apiUri.toString)

  private def executeQuery[Rep: FromResponseUnmarshaller](endpoint: String, httpMethod: HttpMethod): Future[Rep] =
    makePipeline(endpoint)(timeout) flatMap { pipeline =>
      val query = (new RequestBuilder(httpMethod))(endpoint)
      (pipeline ~> unmarshal).apply(query) recoverWith recoverUnsuccessfulResponse
    }

  private def executeQuery[Req: Marshaller, Rep: FromResponseUnmarshaller](endpoint: String, httpMethod: HttpMethod, req: Req): Future[Rep] =
    makePipeline(endpoint)(timeout) flatMap { pipeline =>
      val query = (new RequestBuilder(httpMethod))(endpoint, req)
      (pipeline ~> unmarshal).apply(query) recoverWith recoverUnsuccessfulResponse
    }

  private def makePipeline(uri: String)(implicit timeout: Timeout): Future[SendReceive] =
    (try Future.successful(Uri(uri)) catch {
      case NonFatal(t) => Future.failed(t)
    }) flatMap {
      uri =>
        uri.scheme match {
          case "http" =>
            Future.successful(None)
          case "https" =>
            val hostConnectorSetup = Http.HostConnectorSetup(
              uri.authority.host.address,
              port = if (uri.authority.port == 0) 443 else uri.authority.port,
              sslEncryption = true
            )
            IO(Http).ask(hostConnectorSetup) map {
              case Http.HostConnectorInfo(connector, _) => Some(connector)
            }
        }
    } map {
      case None => sendReceive
      case Some(connector) => sendReceive(connector)
    }

  private def recoverUnsuccessfulResponse[U]: PartialFunction[Throwable, Future[U]] = {
    case ex: UnsuccessfulResponseException =>
      Future.failed[U](
        new WebTranslateItClientException(
          ex.response.status.intValue,
          ex.response.status.reason,
          ex.response.entity.as[Option[WebTranslateItClient.Error]].getOrElse(None)
        )
      )
  }
}