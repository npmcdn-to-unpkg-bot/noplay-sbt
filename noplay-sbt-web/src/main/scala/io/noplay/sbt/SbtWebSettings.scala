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
package io.noplay.sbt

import com.typesafe.sbt.rjs.SbtRjs
import com.typesafe.sbt.web.SbtWeb
import io.alphard.sbt.{SbtWebIndex, SbtWebBrowser, SbtWebServer}
import sbt.AutoPlugin

object SbtWebSettings
  extends AutoPlugin {
  override lazy val requires =
    SbtWeb &&
      SbtWebIndex &&
      SbtRequire &&
      SbtRjs &&
      SbtWebServer &&
      SbtWebBrowser
}