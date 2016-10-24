/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.s3.AmazonS3Client
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties.PropertyKeys._
import org.json4s.native.Serialization.read

import scala.io.Source
import scala.util.{Properties, Success, Try}

object Secrets {
  def readSecrets(): Try[Map[String, Option[String]]] = {
    val amazonClient = new AmazonS3Client(new DefaultAWSCredentialsProviderChain())
    amazonClient.setRegion(Region.getRegion(Regions.EU_CENTRAL_1))

    new Secrets(amazonClient, Properties.envOrElse("NDLA_ENVIRONMENT", "local")).readSecrets()
  }
}

class Secrets(amazonClient: AmazonS3Client, environment: String) extends LazyLogging {
  val SecretsFile = "image_api.secrets"

  def readSecrets(): Try[Map[String, Option[String]]] = {
    implicit val formats = org.json4s.DefaultFormats

    environment match {
      case "local" => Success(Map())
      case _ => {
        val secrets = for {
          s3Object <- Try(amazonClient.getObject(s"$environment.secrets.ndla", SecretsFile))
          fileContent <- Try(Source.fromInputStream(s3Object.getObjectContent).getLines().mkString)
          dbSecrets <- Try(read[Database](fileContent))
        } yield dbSecrets

        secrets.map(s => {
          Map(
            MetaResourceKey -> Some(s.database),
            MetaServerKey -> Some(s.host),
            MetaUserNameKey -> Some(s.user),
            MetaPasswordKey -> Some(s.password),
            MetaPortKey -> Some(s.port),
            MetaSchemaKey -> Some(s.schema)
          )
        })
      }
    }
  }
}

case class Database(database: String,
                    host: String,
                    user: String,
                    password: String,
                    port: String,
                    schema: String)
