/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.repository

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.integration.DataSource
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.service.ConverterService
import org.json4s.native.Serialization.write
import org.postgresql.util.PGobject
import scalikejdbc._


trait ImageRepository {
  this: DataSource with ConverterService =>
  val imageRepository: ImageRepository

  class ImageRepository extends LazyLogging {
    implicit val formats = org.json4s.DefaultFormats + ImageMetaInformation.JSonSerializer

    ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

    def withId(id: Long): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.id = $id")
      }
    }

    def withExternalId(externalId: String): Option[ImageMetaInformation] = {
      DB readOnly { implicit session =>
        imageMetaInformationWhere(sqls"im.external_id = $externalId")
      }
    }

    def insert(imageMetaInformation: ImageMetaInformation, externalId: String): ImageMetaInformation = {
      val json = write(imageMetaInformation)

      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        val imageId = sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})".updateAndReturnGeneratedKey.apply
        imageMetaInformation.copy(id = Some(imageId))
      }
    }

    def update(imageMetaInformation: ImageMetaInformation, id: Long): ImageMetaInformation = {
      val json = write(imageMetaInformation)
      val dataObject = new PGobject()
      dataObject.setType("jsonb")
      dataObject.setValue(json)

      DB localTx { implicit session =>
        sql"update imagemetadata set metadata = ${dataObject} where id = ${id}".update.apply
        imageMetaInformation.copy(id = Some(id))
      }
    }

    def numElements: Int = {
      DB readOnly { implicit session =>
        sql"select count(*) from imagemetadata".map(rs => {
          rs.int("count")
        }).list.first().apply() match {
          case Some(count) => count
          case None => 0
        }
      }
    }

    def applyToAll(func: List[ImageMetaInformation] => Unit) = {
      val im = ImageMetaInformation.syntax("im")
      val numberOfBulks = math.ceil(numElements.toFloat / ImageApiProperties.IndexBulkSize).toInt

      DB readOnly { implicit session =>
        for(i <- 0 until numberOfBulks) {
          func(
            sql"""select ${im.result.*} from ${ImageMetaInformation.as(im)} limit ${ImageApiProperties.IndexBulkSize} offset ${i * ImageApiProperties.IndexBulkSize}""".map(ImageMetaInformation(im)).list.apply()
          )
        }
      }
    }

    private def imageMetaInformationWhere(whereClause: SQLSyntax)(implicit session: DBSession = ReadOnlyAutoSession): Option[ImageMetaInformation] = {
      val im = ImageMetaInformation.syntax("im")
      sql"select ${im.result.*} from ${ImageMetaInformation.as(im)} where $whereClause".map(ImageMetaInformation(im)).single().apply()
    }
  }
}