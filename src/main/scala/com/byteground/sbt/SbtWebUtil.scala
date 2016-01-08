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
    val webUtilName = settingKey[String]("ByTeGround Web Util name")
    val webUtilVersion = settingKey[String]("ByTeGround Web Util version")
    val webUtilModule = settingKey[ModuleID]("ByTeGround Web Util module")

    def webUtilPath(name: String): Def.Setting[_] =
      webUtilPath(name, None)

    def webUtilPath(name: String, path: String): Def.Setting[_] =
      webUtilPath(name, Some(path))

    private def webUtilPath(name: String, path: Option[String]): Def.Setting[_] =
      requireConfigurationPaths += name -> s"/${webModulesLib.value}/${webUtilName.value}/${path.getOrElse(name)}"
  }

  import com.byteground.sbt.SbtWebUtil.autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "bootstrap" -> s"${webModulesLib.value}/bootstrap"
  ) ++ Seq(
    webUtilPath("angular-analytics"),
    webUtilPath("angular-auth", "angular-auth/auth"),
    webUtilPath("angular-autofocus", "angular-autofocus/autofocus"),
    webUtilPath("angular-beforeunload", "angular-beforeunload/beforeunload"),
    webUtilPath("angular-bootstrap-form"),
    webUtilPath("angular-clipboard", "angular-clipboard/clipboard"),
    webUtilPath("angular-debounce", "angular-debounce/debounce"),
    webUtilPath("angular-facebook", "angular-facebook/angular-facebook"),
    webUtilPath("angular-flags", "angular-flags/flags"),
    webUtilPath("angular-form", "angular-form/form"),
    webUtilPath("angular-geo", "angular-geo/geo"),
    webUtilPath("angular-google-chart", "angular-google-chart/google-chart"),
    webUtilPath("angular-link", "angular-link/link"),
    webUtilPath("angular-mediawiki", "angular-mediawiki/mediawiki"),
    webUtilPath("angular-require", "angular-require/require"),
    webUtilPath("angular-scroll", "angular-scroll/scroll"),
    webUtilPath("angular-session", "angular-session/session"),
    webUtilPath("angular-social", "angular-social/social"),
    webUtilPath("angular-time", "angular-time/time"),
    webUtilPath("angular-translate-loader", "angular-translate-loader/loader"),
    webUtilPath("angular-twitter", "angular-twitter/angular-twitter"),
    webUtilPath("angular-ui-bootstrap-clipboard", "angular-ui-bootstrap-clipboard/clipboard"),
    webUtilPath("angular-ui-dialog"),
    webUtilPath("angular-ui-offcanvas"),
    webUtilPath("angular-ui-range", "angular-ui-range"),
    webUtilPath("angular-ui-spinner", "angular-ui-spinner"),
    webUtilPath("angular-unit", "angular-unit/unit"),
    webUtilPath("angular-upload"),
    webUtilPath("angular-url", "angular-url/url"),
    webUtilPath("angular-webstorage", "angular-webstorage/webStorage"),

    webUtilPath("bootloader"),

    webUtilPath("bootstrap-flex", "angular-flex/flex"),
    webUtilPath("bootstrap-ratio", "angular-ratio/ratio"),
    webUtilPath("bootstrap-social", "angular-social/social"),

    webUtilPath("dom"),
    webUtilPath("google"),
    webUtilPath("json/jsonp"),
    webUtilPath("json/jsonrpc"),
    webUtilPath("json/jsonschema"),
    webUtilPath("math"),
    webUtilPath("oauth"),
    webUtilPath("timer"),
    webUtilPath("util", "util/util"),
    webUtilPath("uuid"),

    webUtilPath("async", "requirejs-plugins/async"),
    webUtilPath("css", "requirejs-plugins/css"),
    webUtilPath("depend", "requirejs-plugins/depend"),
    webUtilPath("font", "requirejs-plugins/font"),
    webUtilPath("goog", "requirejs-plugins/goog"),
    webUtilPath("json", "requirejs-plugins/json"),
    webUtilPath("ng", "requirejs-plugins/ng"),
    webUtilPath("smd", "requirejs-plugins/smd"),
    webUtilPath("text", "requirejs-plugins/text"),

    webUtilPath("n"),
    webUtilPath("rpc")
  )

  override lazy val projectSettings = Seq(
    webUtilName := "byteground-web-util",
    webUtilVersion := "0.2.2",
    webUtilModule := "com.byteground" % webUtilName.value % webUtilVersion.value,
    libraryDependencies += webUtilModule.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}