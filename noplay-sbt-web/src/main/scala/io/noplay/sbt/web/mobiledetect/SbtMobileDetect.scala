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
package io.noplay.sbt.web.mobiledetect

import io.noplay.sbt.web.require.SbtRequire
import io.noplay.sbt.web.require.SbtRequire.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import sbt._
import sbt.Keys._

object SbtMobileDetect
  extends AutoPlugin {

  override val requires = SbtRequire

  object autoImport {
    val mobileDetectVersion = settingKey[String]("The mobile detect version")
  }

  import autoImport._

  override val projectSettings = Seq(
    mobileDetectVersion := "1.3.3",
    libraryDependencies += "org.webjars.npm" % "mobile-detect" % mobileDetectVersion.value
  ) ++ inConfig(Assets)(unscopedSettings) ++ inConfig(TestAssets)(unscopedSettings)

  private lazy val unscopedSettings = Seq(
    requireMainConfigPaths += "mobile-detect" -> RequirePath(
      s"/${webModulesLib.value}/mobile-detect/mobile-detect",
      unminifiedCDN = Some(s"//unpkg.com/mobile-detect@${mobileDetectVersion.value}/mobile-detect")
    ).minify()
  )

}