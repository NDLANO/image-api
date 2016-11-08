/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model.domain

import no.ndla.imageapi.ImageApiProperties
import org.json4s.FieldSerializer
import org.json4s.FieldSerializer._
import org.json4s.native.Serialization._
import scalikejdbc._

case class ImageTitle(title: String, language: Option[String])
case class ImageAltText(alttext: String, language: Option[String])
case class ImageCaption(caption: String, language: Option[String])
case class ImageTag(tags: Seq[String], language: Option[String])
case class Image(url: String, size: Int, contentType: String)
case class Copyright(license: License, origin: String, authors: List[Author])
case class License(license: String, description: String, url: Option[String])
case class Author(`type`: String, name: String)
case class ImageMetaInformation(id: Option[Long], titles: Seq[ImageTitle], alttexts: Seq[ImageAltText], imageUrl: String, size: Int, contentType: String, copyright: Copyright, tags: Seq[ImageTag], captions: Seq[ImageCaption])

object ImageMetaInformation extends SQLSyntaxSupport[ImageMetaInformation] {
  implicit val formats = org.json4s.DefaultFormats
  override val tableName = "imagemetadata"
  override val schemaName = Some(ImageApiProperties.MetaSchema)

  def apply(im: SyntaxProvider[ImageMetaInformation])(rs:WrappedResultSet): ImageMetaInformation = apply(im.resultName)(rs)
  def apply(im: ResultName[ImageMetaInformation])(rs: WrappedResultSet): ImageMetaInformation = {
    val meta = read[ImageMetaInformation](rs.string(im.c("metadata")))
    ImageMetaInformation(Some(rs.long(im.c("id"))), meta.titles, meta.alttexts, meta.imageUrl, meta.size, meta.contentType , meta.copyright, meta.tags, meta.captions)
  }

  val JSonSerializer = FieldSerializer[ImageMetaInformation](ignore("id"))
}
