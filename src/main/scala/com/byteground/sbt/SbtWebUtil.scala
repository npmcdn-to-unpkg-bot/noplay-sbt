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

import com.byteground.sbt.SbtRequire.autoImport._
import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import sbt.Keys._
import sbt._

import scala.language.existentials

object SbtWebUtil
  extends AutoPlugin {
  override lazy val requires =
    SbtAngular &&
      SbtBootstrap &&
      SbtQ &&
      SbtJquery

  object autoImport {
    val webUtilName = settingKey[String]( "ByTeGround Web Util name" )
    val webUtilVersion = settingKey[String]( "ByTeGround Web Util version" )

    def webUtilPath(name: String): Def.Setting[_] =
      webUtilPath(name, None)

    def webUtilPath(name: String, path: String): Def.Setting[_] =
      webUtilPath(name, Some(path))

    private def webUtilPath(name: String, path: Option[String]): Def.Setting[_] =
      requireConfigurationPaths += name -> s"${webModulesLib.value}/${webUtilName.value}/${path.getOrElse(name)}"
  }

  import com.byteground.sbt.SbtWebUtil.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "bootstrap" -> s"${webModulesLib.value}/bootstrap"
  ) ++ Seq(
    webUtilPath("async", "requirejs-plugins/async"),
    webUtilPath("css", "requirejs-plugins/css"),
    webUtilPath("depend", "requirejs-plugins/depend"),
    webUtilPath("font", "requirejs-plugins/font"),
    webUtilPath("goog", "requirejs-plugins/goog"),
    webUtilPath("json", "requirejs-plugins/json"),
    webUtilPath("ng", "requirejs-plugins/ng"),
    webUtilPath("smd", "requirejs-plugins/smd"),
    webUtilPath("text", "requirejs-plugins/text"),

    webUtilPath("angular-ui-offcanvas"),
    webUtilPath("angular-appstorage", "angular-appstorage/appStorage"),
    webUtilPath("angular-analytics"),
    webUtilPath("angular-auth", "angular-auth/auth"),
    webUtilPath("angular-session", "angular-session/session"),
    webUtilPath("angular-webstorage", "angular-webstorage/webStorage"),
    webUtilPath("angular-time", "angular-time/time"),

    webUtilPath("json/jsonschema"),
    webUtilPath("json/jsonrpc"),
    webUtilPath("json/jsonp"),
    webUtilPath("rpc"),

    webUtilPath("google")
  )

  override lazy val projectSettings = Seq(
    webUtilName := "byteground-web-util",
    webUtilVersion := "0.2.2",
    libraryDependencies += "com.byteground" % webUtilName.value % webUtilVersion.value
  ) ++ inConfig( Assets )( unscopedProjectSettings ) ++ inConfig( TestAssets )( unscopedProjectSettings )
}