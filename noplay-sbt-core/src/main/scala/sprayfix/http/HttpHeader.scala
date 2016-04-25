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

import spray.http._
import spray.util._

object HttpHeaders {
  sealed abstract class ModeledCompanion(val name: String) extends Renderable {
    val lowercaseName = name.toLowerCase
    private[this] val nameBytes = name.getAsciiBytes
    def render[R <: Rendering](r: R): r.type = r ~~ nameBytes ~~ ':' ~~ ' '
  }

  sealed abstract class ModeledHeader extends HttpHeader with Serializable {
    def name: String = companion.name
    def value: String = renderValue(new StringRendering).get
    def lowercaseName: String = companion.lowercaseName
    def render[R <: Rendering](r: R): r.type = renderValue(r ~~ companion)
    def renderValue[R <: Rendering](r: R): r.type
    protected def companion: ModeledCompanion
  }

  object `Content-Disposition` extends ModeledCompanion("Content-Disposition")
  case class `Content-Disposition`(dispositionType: String, parameters: Map[String, String] = Map.empty) extends ModeledHeader {
    def renderValue[R <: Rendering](r: R): r.type = {
      r ~~ dispositionType
      if (parameters.nonEmpty) parameters foreach { case (k, v) ⇒ r ~~ ';' ~~ ' ' ~~ k ~~ '=' ~~#! v }
      r
    }
    protected def companion = `Content-Disposition`
  }
}