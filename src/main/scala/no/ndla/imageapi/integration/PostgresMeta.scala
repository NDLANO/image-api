package no.ndla.imageapi.integration

import javax.sql.DataSource

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.business.ImageMeta
import no.ndla.imageapi.model.{Image, ImageMetaInformation, ImageMetaSummary, ImageVariants}
import org.postgresql.util.PGobject
import scalikejdbc._


class PostgresMeta(dataSource: DataSource) extends ImageMeta with LazyLogging {
  val UrlPrefix = "http://api.test.ndla.no/images/"

  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  override def withId(id: String): Option[ImageMetaInformation] = {
    import org.json4s.native.Serialization.read
    implicit val formats = org.json4s.DefaultFormats

    DB readOnly {implicit session =>
      sql"select metadata from imagemetadata where id = ${id.toInt}".map(rs => rs.string("metadata")).single().apply() match {
        case Some(json) => {
          val meta = read[ImageMetaInformation](json)

          Option(ImageMetaInformation(
            id,
            meta.titles,
            ImageVariants(
              meta.images.small.flatMap(s => Option(Image(UrlPrefix + s.url, s.size, s.contentType))),
              meta.images.full.flatMap(f => Option(Image(UrlPrefix + f.url, f.size, f.contentType)))),
            meta.copyright,
            meta.tags))
        }
        case None => None
      }
    }
  }

  override def insert(imageMetaInformation: ImageMetaInformation, externalId: String): Unit = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(imageMetaInformation)

    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"insert into imagemetadata(external_id, metadata) values(${externalId}, ${dataObject})".update().apply()
    }
  }

  override def update(imageMetaInformation: ImageMetaInformation, externalId: String): Unit = {
    import org.json4s.native.Serialization.write
    implicit val formats = org.json4s.DefaultFormats

    val json = write(imageMetaInformation)
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(json)

    DB localTx {implicit session =>
      sql"update imagemetadata set metadata = ${dataObject} where external_id = ${externalId}".update().apply()
    }
  }

  override def containsExternalId(externalId: String): Boolean = {
    DB readOnly{implicit session =>
      sql"select id from imagemetadata where external_id = ${externalId}".map(rs => rs.long("id")).single().apply().isDefined
    }
  }
}
