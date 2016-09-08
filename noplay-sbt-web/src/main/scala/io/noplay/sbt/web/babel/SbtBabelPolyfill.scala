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
package io.noplay.sbt.web.babel

import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import io.noplay.sbt.web.require.SbtRequire
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import sbt.Keys._
import sbt._

object SbtBabelPolyfill
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val babelPolyfillVersion = settingKey[String]("The babel polyfill version")
  }

  import autoImport._

  override lazy val projectSettings = Seq(
    babelPolyfillVersion := "6.13.0",
    libraryDependencies += "org.webjars.npm" % "babel-polyfill" % babelPolyfillVersion.value intransitive()
  ) ++ inConfig(Assets)(unscopedSettings) ++ inConfig(TestAssets)(unscopedSettings)

  private lazy val unscopedSettings = Seq(
    requireMainConfigPaths += "babel-polyfill" -> RequirePath(
      s"/${webModulesLib.value}/babel-polyfill/dist/polyfill",
      unminifiedCDN = Some(s"//npmcdn.com/babel-polyfill@${babelPolyfillVersion.value}/dist/polyfill")
    ).minify(".min")
  )

}