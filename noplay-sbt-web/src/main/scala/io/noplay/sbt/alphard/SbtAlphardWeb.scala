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
package io.noplay.sbt.alphard

import com.typesafe.sbt.web.Import.WebKeys._
import com.typesafe.sbt.web.Import._
import io.noplay.sbt.SbtRequire.autoImport._
import io.noplay.sbt.angular.SbtAngular
import io.noplay.sbt.bootstrap.SbtBootstrap
import io.noplay.sbt.{SbtJquery, SbtQ}
import sbt.Keys._
import sbt._

import scala.language.existentials

object SbtAlphardWeb
  extends AutoPlugin {
  override val requires =
    SbtAngular &&
      SbtBootstrap &&
      SbtQ &&
      SbtJquery

  private val Organization = "io.alphard"
  private val Name = "alphard-web"

  object autoImport {
    val alphardWebVersion = settingKey[String]("Alphard Web version")

    def alphardWebPath(name: String): Def.Setting[_] =
      alphardWebPath(name, None)

    def alphardWebPath(name: String, path: String): Def.Setting[_] =
      alphardWebPath(name, Some(path))

    private def alphardWebPath(name: String, path: Option[String]): Def.Setting[_] =
      requireConfigurationPaths += name -> s"/${webModulesLib.value}/$Name/${path.getOrElse(name)}"
  }

  import autoImport._

  val unscopedProjectSettings = Seq(
    requireConfigurationPaths += "bootstrap" -> s"${webModulesLib.value}/bootstrap"
  ) ++ Seq(
    alphardWebPath("angular-analytics"),
    alphardWebPath("angular-auth", "angular-auth/auth"),
    alphardWebPath("angular-autofocus", "angular-autofocus/autofocus"),
    alphardWebPath("angular-beforeunload", "angular-beforeunload/beforeunload"),
    alphardWebPath("angular-bootstrap-form"),
    alphardWebPath("angular-clipboard", "angular-clipboard/clipboard"),
    alphardWebPath("angular-debounce", "angular-debounce/debounce"),
    alphardWebPath("angular-facebook", "angular-facebook/angular-facebook"),
    alphardWebPath("angular-flags", "angular-flags/flags"),
    alphardWebPath("angular-form", "angular-form/form"),
    alphardWebPath("angular-geo", "angular-geo/geo"),
    alphardWebPath("angular-google-chart", "angular-google-chart/google-chart"),
    alphardWebPath("angular-link", "angular-link/link"),
    alphardWebPath("angular-mediawiki", "angular-mediawiki/mediawiki"),
    alphardWebPath("angular-require", "angular-require/require"),
    alphardWebPath("angular-scroll", "angular-scroll/scroll"),
    alphardWebPath("angular-session", "angular-session/session"),
    alphardWebPath("angular-social", "angular-social/social"),
    alphardWebPath("angular-time", "angular-time/time"),
    alphardWebPath("angular-translate-loader", "angular-translate-loader/loader"),
    alphardWebPath("angular-twitter", "angular-twitter/angular-twitter"),
    alphardWebPath("angular-ui-bootstrap-clipboard", "angular-ui-bootstrap-clipboard/clipboard"),
    alphardWebPath("angular-ui-dialog"),
    alphardWebPath("angular-ui-offcanvas"),
    alphardWebPath("angular-ui-range", "angular-ui-range"),
    alphardWebPath("angular-ui-spinner", "angular-ui-spinner"),
    alphardWebPath("angular-unit", "angular-unit/unit"),
    alphardWebPath("angular-upload"),
    alphardWebPath("angular-url", "angular-url/url"),
    alphardWebPath("angular-webstorage", "angular-webstorage/webStorage"),

    alphardWebPath("bootloader"),

    alphardWebPath("bootstrap-flex", "angular-flex/flex"),
    alphardWebPath("bootstrap-ratio", "angular-ratio/ratio"),
    alphardWebPath("bootstrap-social", "angular-social/social"),

    alphardWebPath("dom"),
    alphardWebPath("google"),
    alphardWebPath("json/jsonp"),
    alphardWebPath("json/jsonrpc"),
    alphardWebPath("json/jsonschema"),
    alphardWebPath("math"),
    alphardWebPath("oauth"),
    alphardWebPath("timer"),
    alphardWebPath("logger", "logger/logger"),
    alphardWebPath("util", "util/util"),
    alphardWebPath("uuid"),

    alphardWebPath("async", "requirejs-plugins/async"),
    alphardWebPath("css", "requirejs-plugins/css"),
    alphardWebPath("depend", "requirejs-plugins/depend"),
    alphardWebPath("font", "requirejs-plugins/font"),
    alphardWebPath("goog", "requirejs-plugins/goog"),
    alphardWebPath("json", "requirejs-plugins/json"),
    alphardWebPath("ng", "requirejs-plugins/ng"),
    alphardWebPath("smd", "requirejs-plugins/smd"),
    alphardWebPath("text", "requirejs-plugins/text"),

    alphardWebPath("n"),
    alphardWebPath("rpc")
  )

  override val projectSettings = Seq(
    alphardWebVersion := "0.18.0",
    libraryDependencies += Organization % Name % alphardWebVersion.value
  ) ++ inConfig(Assets)(unscopedProjectSettings) ++ inConfig(TestAssets)(unscopedProjectSettings)
}