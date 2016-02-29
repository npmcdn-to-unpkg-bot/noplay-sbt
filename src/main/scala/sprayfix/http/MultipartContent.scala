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
package sprayfix.http

import java.io.File

import spray.http._
import sprayfix.http.HttpHeaders._

trait MultipartParts {
  def parts: Seq[BodyPart]
}

case class MultipartContent(parts: Seq[BodyPart]) extends MultipartParts
object MultipartContent {
  val Empty = MultipartContent(Nil)
}

case class MultipartByteRanges(parts: Seq[BodyPart]) extends MultipartParts
object MultipartByteRanges {
  val Empty = MultipartByteRanges(Nil)
}

case class BodyPart(entity: HttpEntity, headers: Seq[HttpHeader] = Nil) {
  val name: Option[String] = dispositionParameterValue("name")

  def filename: Option[String] = dispositionParameterValue("filename")
  def disposition: Option[String] =
    headers.collectFirst {
      case disposition: `Content-Disposition` => disposition.dispositionType
    }

  def dispositionParameterValue(parameter: String): Option[String] =
    headers.collectFirst {
      case `Content-Disposition`("form-data", parameters) if parameters.contains(parameter) ⇒
        parameters(parameter)
    }

  def contentRange: Option[ContentRange] =
    headers.collectFirst {
      case contentRangeHeader: spray.http.HttpHeaders.`Content-Range` ⇒ contentRangeHeader.contentRange
    }
}
object BodyPart {
  def apply(file: File, fieldName: String): BodyPart = apply(file, fieldName, ContentTypes.`application/octet-stream`)
  def apply(file: File, fieldName: String, contentType: ContentType): BodyPart =
    apply(HttpEntity(contentType, HttpData(file)), fieldName, Map.empty.updated("filename", file.getName))

  def apply(formFile: FormFile, fieldName: String): BodyPart =
    formFile.name match {
      case Some(name) ⇒ apply(formFile.entity, fieldName, Map.empty.updated("filename", name))
      case None       ⇒ apply(formFile.entity, fieldName)
    }

  def apply(entity: HttpEntity, fieldName: String): BodyPart = apply(entity, fieldName, Map.empty[String, String])
  def apply(entity: HttpEntity, fieldName: String, parameters: Map[String, String]): BodyPart =
    BodyPart(entity, Seq(`Content-Disposition`("form-data", parameters.updated("name", fieldName))))
}