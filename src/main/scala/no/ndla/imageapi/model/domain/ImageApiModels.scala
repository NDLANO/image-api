/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import java.util.Date

import no.ndla.imageapi.ImageApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class ImageTitle(title: String, language: String) extends LanguageField[String] { override def value: String = title }
case class ImageAltText(alttext: String, language: String) extends LanguageField[String] { override def value: String = alttext }
case class ImageCaption(caption: String, language: String) extends LanguageField[String] { override def value: String = caption }
case class ImageTag(tags: Seq[String], language: String) extends LanguageField[Seq[String]] { override def value: Seq[String] = tags }
case class Image(fileName: String, size: Long, contentType: String)
case class Copyright(license: License, origin: String, creators: Seq[Author], processors: Seq[Author], rightsholders: Seq[Author], validFrom: Option[Date], validTo: Option[Date])
case class License(license: String, description: String, url: Option[String])
case class Author(`type`: String, name: String)
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
  updated: Date
)

object ImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "imagemetadata"
  override val schemaName = Some(ImageApiProperties.MetaSchema)
  val JSonSerializer = FieldSerializer[ImageMetaInformation](ignore("id"))

  def apply(im: SyntaxProvider[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = apply(im.resultName)(rs)

  def apply(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
    val meta = read[ImageMetaInformation](rs.string(im.c("metadata")))
    ImageMetaInformation(Some(rs.long(im.c("id"))), meta.titles, meta.alttexts, meta.imageUrl, meta.size, meta.contentType,
      meta.copyright, meta.tags, meta.captions, meta.updatedBy, meta.updated)
  }
}

case class ReindexResult(totalIndexed: Int, millisUsed: Long)