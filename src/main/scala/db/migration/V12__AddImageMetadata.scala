/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import com.typesafe.scalalogging.LazyLogging
import io.lemonlabs.uri.Uri
import no.ndla.imageapi.ImageApiProperties.StorageName
import org.apache.commons.io.IOUtils
import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.JsonAST.JField
import org.json4s.{DefaultFormats, JLong, JObject}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, _}

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import scala.util.{Failure, Success, Try}

class V12__AddImageMetadata extends BaseJavaMigration with LazyLogging {

  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(context: Context) = {
    val db = DB(context.getConnection)
    db.autoClose(false)

    db.withinTx { implicit session =>
      imagesToUpdate.map {
        case (id, document) => update(convertImageUpdate(document), id)
      }
    }
  }

  def imagesToUpdate(implicit session: DBSession): List[(Long, String)] = {
    sql"select id, metadata from imagemetadata"
      .map(rs => {
        (rs.long("id"), rs.string("metadata"))
      })
      .list()
  }

  val currentRegion: Option[Regions] = Option(Regions.getCurrentRegion).map(region => Regions.fromName(region.getName))

  val amazonClient =
    AmazonS3ClientBuilder
      .standard()
      .withRegion(currentRegion.getOrElse(Regions.EU_CENTRAL_1))
      .build()

  def get(imageKey: String): Try[BufferedImage] = {
    val res = Try(amazonClient.getObject(new GetObjectRequest(StorageName, imageKey)))
    res.map(s3Object => {
      val imageContent = {
        val content = IOUtils.toByteArray(s3Object.getObjectContent)
        s3Object.getObjectContent.close()
        content
      }

      val stream = new ByteArrayInputStream(imageContent)
      ImageIO.read(stream)
    })
  }

  def convertImageUpdate(imageMeta: String): String = {
    val oldDocument = parse(imageMeta)

    val imageUrl = (oldDocument \ "imageUrl").extract[String]
    val imageKey = Uri
      .parse(imageUrl)
      .toStringRaw
      .dropWhile(_ == '/') // Strip heading '/'
    val mergeObject = get(imageKey) match {
      case Success(image) => {
        JObject(
          JField("width", JLong(image.getWidth())),
          JField("height", JLong(image.getHeight()))
        )
      }
      case Failure(ex) => {
        logger.warn(s"Something went wrong when fetching $imageKey", ex)
        JObject()
      }
    }

    val mergedDoc = oldDocument.merge(mergeObject)
    compact(render(mergedDoc))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update()
  }
}
