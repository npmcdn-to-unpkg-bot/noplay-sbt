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

import sprayfix.http.HttpHeaders._

sealed trait HttpForm {
  type FieldType
  def fields: Seq[FieldType]
}

case class FormData(fields: Seq[(String, String)]) extends HttpForm {
  type FieldType = (String, String)
}

object FormData {
  val Empty = FormData(Seq.empty)
  def apply(fields: Map[String, String]): FormData = this(fields.toSeq)
}

case class MultipartFormData(fields: Seq[BodyPart]) extends HttpForm {
  type FieldType = BodyPart
  def get(partName: String): Option[BodyPart] = fields.find(_.name.exists(_ == partName))
}

object MultipartFormData {
  val Empty = MultipartFormData(Seq.empty)
  def apply(fields: Map[String, BodyPart]): MultipartFormData = this{
    fields.map {
      case (key, value) ⇒ value.copy(headers = `Content-Disposition`("form-data", Map("name" -> key)) +: value.headers)
    }(collection.breakOut)
  }
}