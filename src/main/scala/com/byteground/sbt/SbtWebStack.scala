/**
 * Copyright © 2009-2014 ByTeGround, Inc
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

import com.typesafe.sbt.jse.SbtJsEngine
import com.typesafe.sbt.less.SbtLess
import com.typesafe.sbt.rjs.SbtRjs
import com.typesafe.sbt.web.SbtWeb
import sbt.AutoPlugin

object SbtWebStack
  extends AutoPlugin {
  override lazy val requires =
    SbtWeb &&
      SbtJsEngine &&
      SbtWebIndex &&
      SbtRequireJs &&
      SbtJquery &&
      SbtQ &&
      SbtLess &&
      SbtAngularJs &&
      SbtAngularUiRouter &&
      SbtBootstrapJs &&
      SbtAngularUiBootstrap &&
      SbtFontAwesome &&
      SbtWebUtil &&
      SbtRjs &&
      SbtWebServer &&
      SbtWebBrowser
}