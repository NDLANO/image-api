package db.migration

import org.flywaydb.core.api.migration.{BaseJavaMigration, Context}
import org.json4s.native.JsonMethods.{compact, parse, render}
import org.json4s.{DefaultFormats, Extraction}
import org.postgresql.util.PGobject
import scalikejdbc.{DB, DBSession, scalikejdbcSQLInterpolationImplicitDef}

class V11__ConvertLanguageUnknown extends BaseJavaMigration {
  implicit val formats: DefaultFormats.type = org.json4s.DefaultFormats
  val timeService = new TimeService()

  override def migrate(context: Context): Unit = {
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

  def convertImageUpdate(imageMeta: String): String = {
    val oldImage = parse(imageMeta)
    val extractedAudio = oldImage.extract[V11_ImageMetaInformation]
    val tags = extractedAudio.tags.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val titles = extractedAudio.titles.map(t => {
      if (t.language == "unknown")
        t.copy(language = "und")
      else
        t
    })
    val captions = extractedAudio.captions.map(c => {
      if (c.language == "unknown")
        c.copy(language = "und")
      else
        c
    })
    val alttexts = extractedAudio.alttexts.map(a => {
      if (a.language == "unknown")
        a.copy(language = "und")
      else
        a
    })
    val updated = oldImage
      .replace(List("tags"), Extraction.decompose(tags))
      .replace(List("titles"), Extraction.decompose(titles))
      .replace(List("captions"), Extraction.decompose(captions))
      .replace(List("alttexts"), Extraction.decompose(alttexts))
    compact(render(updated))
  }

  def update(imagemetadata: String, id: Long)(implicit session: DBSession) = {
    val dataObject = new PGobject()
    dataObject.setType("jsonb")
    dataObject.setValue(imagemetadata)

    sql"update imagemetadata set metadata = ${dataObject} where id = $id".update()
  }

  case class V11_ImageTitle(title: String, language: String)
  case class V11_ImageAltText(alttext: String, language: String)
  case class V11_ImageCaption(caption: String, language: String)
  case class V11_ImageTag(tags: Seq[String], language: String)
  case class V11_ImageMetaInformation(
      titles: Seq[V11_ImageTitle],
      alttexts: Seq[V11_ImageAltText],
      tags: Seq[V11_ImageTag],
      captions: Seq[V11_ImageCaption],
  )
}
