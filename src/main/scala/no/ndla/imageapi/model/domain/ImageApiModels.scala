/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import java.util.Date
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import org.json4s.{DefaultFormats, FieldSerializer, Formats}
import org.json4s.FieldSerializer._
import org.json4s.ext.EnumNameSerializer
import org.json4s.native.Serialization._
import scalikejdbc._

import scala.util.{Failure, Success, Try}

case class ImageTitle(title: String, language: String) extends LanguageField[String] {
  override def value: String = title
}
case class ImageAltText(alttext: String, language: String) extends LanguageField[String] {
  override def value: String = alttext
}
case class ImageCaption(caption: String, language: String) extends LanguageField[String] {
  override def value: String = caption
}
case class ImageTag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] {
  override def value: Seq[String] = tags
}
case class Image(fileName: String, size: Long, contentType: String)
case class Copyright(license: String,
                     origin: String,
                     creators: Seq[Author],
                     processors: Seq[Author],
                     rightsholders: Seq[Author],
                     agreementId: Option[Long],
                     validFrom: Option[Date],
                     validTo: Option[Date])
case class License(license: String, description: String, url: Option[String])
case class Author(`type`: String, name: String)
case class EditorNote(timeStamp: Date, updatedBy: String, note: String)

object ModelReleasedStatus extends Enumeration {
  val YES = Value("yes")
  val NO = Value("no")
  val NOT_APPLICABLE = Value("not-applicable")

  def valueOfOrError(s: String): Try[this.Value] =
    valueOf(s) match {
      case Some(st) => Success(st)
      case None =>
        val validStatuses = values.map(_.toString).mkString(", ")
        Failure(
          new ValidationException(
            errors = Seq(
              ValidationMessage("modelReleased",
                                s"'$s' is not a valid model released status. Must be one of $validStatuses"))))
    }

  def valueOf(s: String): Option[this.Value] = values.find(_.toString == s)
}

case class ImageMetaInformation(
    id: Option[Long],
    titles: Seq[ImageTitle],
    alttexts: Seq[ImageAltText],
    imageUrl: String,
    size: Long,
    contentType: String,
    copyright: Copyright,
    tags: Seq[ImageTag],
    captions: Seq[ImageCaption],
    updatedBy: String,
    updated: Date,
    created: Date,
    createdBy: String,
    modelReleased: ModelReleasedStatus.Value,
    editorNotes: Seq[EditorNote]
)

object ImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
  override val tableName = "imagemetadata"
  override val schemaName = Some(ImageApiProperties.MetaSchema)
  val jsonEncoder: Formats = DefaultFormats + new EnumNameSerializer(ModelReleasedStatus)
  val repositorySerializer = jsonEncoder + FieldSerializer[ImageMetaInformation](ignore("id"))

  def fromResultSet(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation =
    fromResultSet(im.resultName)(rs)

  def fromResultSet(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
    implicit val formats = this.jsonEncoder
    val id = rs.long(im.c("id"))
    val jsonString = rs.string(im.c("metadata"))
    val meta = read[ImageMetaInformation](jsonString)
    ImageMetaInformation(
      Some(id),
      meta.titles,
      meta.alttexts,
      meta.imageUrl,
      meta.size,
      meta.contentType,
      meta.copyright,
      meta.tags,
      meta.captions,
      meta.updatedBy,
      meta.updated,
      meta.created,
      meta.createdBy,
      meta.modelReleased,
      meta.editorNotes
    )
  }
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)
