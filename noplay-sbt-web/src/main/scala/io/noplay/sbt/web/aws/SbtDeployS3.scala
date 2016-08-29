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

import com.typesafe.sbt.web.SbtWeb.autoImport.WebKeys._
import com.typesafe.sbt.web._
import io.alphard.sbt.aws.SbtAws
import io.alphard.sbt.aws.SbtAws.autoImport._
import io.alphard.sbt.aws.SbtS3.autoImport._
import sbt.Keys._
import sbt._

object SbtDeployS3
  extends AutoPlugin {

  override lazy val requires = SbtWeb && SbtAws

  object autoImport {
    val DeployS3 = config("deploy-s3").hide
    val DeployS3Realm = "Deploy S3 Realm"
    val deployS3 = taskKey[Seq[(File, URI)]]("Deploy the staging directory content to an s3 bucket")
  }

  import SbtDeployS3.autoImport._

  override lazy val projectSettings =
    inTask(deployS3)(awsSettings) ++
      inTask(deployS3)(s3Settings) ++ Seq(
      awsRealm in deployS3 := DeployS3Realm,
      s3Directory in deployS3 := stagingDirectory.value,
      s3BucketName in deployS3 := name.value,
      deployS3 := (s3Sync in deployS3).dependsOn(stage).value
    )
}