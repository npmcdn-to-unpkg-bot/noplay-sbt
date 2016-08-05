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
package io.noplay.sbt.web.aws

import com.amazonaws.services.s3.model.CannedAccessControlList
import io.alphard.sbt.aws.SbtAws.autoImport._
import io.alphard.sbt.aws.SbtS3
import io.alphard.sbt.aws.SbtS3.autoImport._
import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web._
import sbt.Keys._
import sbt._

object SbtDeployS3
  extends AutoPlugin {

  override lazy val requires = SbtWeb && SbtS3

  object autoImport {
    val DeployS3 = config("deploy-s3").hide
    val DeployS3Realm = "Deploy S3 Realm"

    val deployS3Realm = settingKey[String]("S3 realm")
    val deployS3BucketName = taskKey[String]("S3 bucket name")
    val deployS3IndexName = settingKey[String]("S3 index name")
    val deployS3ErrorName = settingKey[String]("S3 error name")
    val deployS3 = taskKey[Seq[URL]]("Deploy the staging directory content to an s3 bucket")
  }

  import SbtDeployS3.autoImport._

  override lazy val projectSettings =
    inConfig(DeployS3)(awsSettings) ++
      inConfig(DeployS3)(s3Settings) ++ Seq(
      deployS3Realm := DeployS3Realm,
      deployS3BucketName := name.value,
      deployS3IndexName := "index.html",
      deployS3ErrorName := "error.html",
      deployS3 <<= s3Sync in DeployS3,
      awsRealm in DeployS3 <<= deployS3Realm,
      s3Cacl in DeployS3 := CannedAccessControlList.PublicRead,
      s3BucketName in DeployS3 <<= deployS3BucketName,
      s3Mapping in DeployS3 := {
        val directory = stagingDirectory.value
        val files = IO.listFiles(stage.value)
        io.alphard.sbt.util.IO.mapAllFiles(Seq(directory), files).toSeq
      }
    )
}