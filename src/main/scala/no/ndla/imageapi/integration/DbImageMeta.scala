/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.integration

import javax.sql.DataSource

import no.ndla.imageapi.model._
import no.ndla.imageapi.model.db._
import no.ndla.imageapi.business.ImageMeta
import slick.driver.PostgresDriver

import slick.driver.PostgresDriver.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DbImageMeta(dataSource: DataSource) extends ImageMeta {

  val PageSize = 20
  val PageSizeSearch = 100
  val UrlPrefix = "http://api.test.ndla.no/images/"

  override def all(minimumSize:Option[Int], license: Option[String]): Iterable[ImageMetaSummary] = {
    val db = Database.forDataSource(dataSource)
    try {
      val allFilter = Tables.imagemetas

      val licenseFilter = license match {
        case Some(license) => allFilter.filter(_.license === license)
        case None => allFilter
      }

      val sizeFilter = minimumSize match {
        case Some(size) =>
          allFilter.flatMap(
            meta => Tables.images
              .filter(image => meta.fullId === image.id)
              .filter(image => image.size >= size)
              .map(image => meta))

        case None => licenseFilter
      }

      val query = sizeFilter.take(PageSize)
      val result = Await.result(db.run(query.result), Duration.Inf)

      result.map(mapImageSummary(_, db))
    } finally db.close()
  }

  override def withId(id: String): Option[ImageMetaInformation] = {
    val db = Database.forDataSource(dataSource)
    try {
      val query = Tables.imagemetas.filter(_.id === id.toLong)
      val result = Await.result(db.run(query.result.headOption), Duration.Inf)

      result.map(mapImage(_, db))
    } finally db.close()
  }

  override def withTags(tagList: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String]): Iterable[ImageMetaSummary] = {
    val db = Database.forDataSource(dataSource)
    try {

      val languageFilter = language match {
        case Some(lang) => Tables.imagetags.filter(_.language === lang)
        case None => Tables.imagetags
      }

      val tagFilter = languageFilter.filter(_.tag.toLowerCase inSet tagList).flatMap(_.imageMeta).groupBy(x=>x).map(_._1)
      val sizeFilter = minimumSize match {
        case Some(size) => tagFilter.flatMap(meta => Tables.images
          .filter(image => meta.fullId === image.id)
          .filter(image => image.size >= size)
          .map(image => meta))

        case None => tagFilter
      }

      val filter = license match {
        case Some(license) => sizeFilter.filter(_.license === license)
        case None => sizeFilter
      }


      val query = filter.take(PageSizeSearch)
      val result = Await.result(db.run(query.result), Duration.Inf)

      result.map(mapImageSummary(_, db))
    } finally db.close()
  }

  override def containsExternalId(externalId: String): Boolean = {
    val db = Database.forDataSource(dataSource)
    try {
      Await.result(db.run(
        Tables.imagemetas.filter(_.externalId === externalId).result.headOption), Duration.Inf).isDefined
    } finally db.close()
  }

  def mapImageSummary(imageMeta: Tables.ImageMeta, db:PostgresDriver.backend.Database): ImageMetaSummary = {
    val previewUrl = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.smallId).result.headOption), Duration.Inf).
      map(img => UrlPrefix + img.url).getOrElse("")

    val metaUrl = UrlPrefix + imageMeta.id.toString

    ImageMetaSummary(imageMeta.id.toString, previewUrl, metaUrl, imageMeta.license)
  }

  def mapImage(imageMeta: Tables.ImageMeta, db:PostgresDriver.backend.Database): ImageMetaInformation = {
    val fullImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.fullId).result.headOption), Duration.Inf).
      map(img => Image(UrlPrefix + img.url, img.size,img.contentType))

    val smallImage = Await.result(db.run(
      Tables.images.filter(_.id === imageMeta.smallId).result.headOption), Duration.Inf).
      map(img => Image(UrlPrefix + img.url, img.size,img.contentType))

    val imageAuthors = Await.result(db.run(
      Tables.authors.filter(_.imageMetaId === imageMeta.id).result), Duration.Inf).
      map(author => Author(author.typeAuthor, author.name))

    val tags = Await.result(db.run(
      Tables.imagetags.filter(_.imageMetaId === imageMeta.id).result), Duration.Inf).
      map(dbtag => ImageTag(dbtag.tag, dbtag.language))

    val titles = Await.result(db.run(
      Tables.imagetitles.filter(_.imageMetaId === imageMeta.id).result), Duration.Inf).
      map(dbimage => ImageTitle(dbimage.title, dbimage.language))

    val license = Await.result(db.run(
      Tables.imagelicenses.filter(_.id === imageMeta.license).result.headOption), Duration.Inf)
      .map(dblicense => License(dblicense.id, dblicense.description, dblicense.url))
      .getOrElse(License(imageMeta.license, imageMeta.license, None))

    ImageMetaInformation(imageMeta.id.toString, titles,
      ImageVariants(smallImage, fullImage),
      Copyright(license, imageMeta.origin, imageAuthors),
      tags
    )
  }

  override def insert(imageMetaInformation: ImageMetaInformation, externalId: String) = {
    val db = Database.forDataSource(dataSource)
    try {

      val fullImage = imageMetaInformation.images.full.get
      val fullInsert = (Tables.images returning Tables.images.map(_.id)) += Tables.Image(0, fullImage.url, fullImage.size, fullImage.contentType)
      val fullId = Await.result(db.run(fullInsert), Duration.Inf)

      val thumbImage = imageMetaInformation.images.small.get
      val thumbInsert = (Tables.images returning Tables.images.map(_.id)) += Tables.Image(0, thumbImage.url, thumbImage.size, thumbImage.contentType)
      val thumbId = Await.result(db.run(thumbInsert), Duration.Inf)

      val insertImageMeta = Tables.ImageMeta(0, imageMetaInformation.copyright.license.license, imageMetaInformation.copyright.origin, thumbId, fullId, externalId)
      val imageMetaId = Await.result(db.run((Tables.imagemetas returning Tables.imagemetas.map(_.id)) += insertImageMeta), Duration.Inf)

      imageMetaInformation.titles.foreach(title => {
        Await.result(db.run(Tables.imagetitles += Tables.ImageTitle(0, title.title, title.language, imageMetaId)), Duration.Inf)
      })

      imageMetaInformation.tags.foreach(tag => {
        Await.result(db.run(Tables.imagetags += Tables.ImageTag(0, tag.tag, tag.language, imageMetaId)), Duration.Inf)
      })

      imageMetaInformation.copyright.authors.foreach(author => {
        Await.result(db.run(Tables.authors += Tables.ImageAuthor(0, author.`type`, author.name, imageMetaId)), Duration.Inf)
      })


    } finally db.close()
  }

  override def update(imageMetaInformation: ImageMetaInformation, externalId: String) = {
    val db = Database.forDataSource(dataSource)
    try {

      val imageMeta = Await.result(db.run(Tables.imagemetas.filter(_.externalId === externalId).result.headOption), Duration.Inf)
      imageMeta.foreach(existingImageMeta => {

        imageMetaInformation.titles.foreach(title => {
          if(!containsTitle(title, existingImageMeta.id)){
            Await.result(db.run(Tables.imagetitles += Tables.ImageTitle(0, title.title, title.language, existingImageMeta.id)), Duration.Inf)
          }
        })

        imageMetaInformation.tags.foreach(tag => {
          if(!containsTag(tag, existingImageMeta.id)){
            Await.result(db.run(Tables.imagetags += Tables.ImageTag(0, tag.tag, tag.language, existingImageMeta.id)), Duration.Inf)
          }
        })
      })
    } finally db.close()
  }

  def containsTitle(title: ImageTitle, imageMetaId: Long): Boolean = {
    val db = Database.forDataSource(dataSource)
    try {

      val query = Tables.imagetitles
        .filter(_.imageMetaId === imageMetaId)
        .filter(m => (m.language.?.isEmpty && title.language.isEmpty) || (m.language === title.language))
        .filter(_.title === title.title).result.headOption

      Await.result(db.run(query),
        Duration.Inf).isDefined

    } finally db.close()
  }

  def containsTag(tag: ImageTag, imageMetaId: Long): Boolean = {
    val db = Database.forDataSource(dataSource)
    try {
      Await.result(db.run(Tables.imagetags
        .filter(_.imageMetaId === imageMetaId)
        .filter(m => (m.language.?.isEmpty && tag.language.isEmpty) || (m.language === tag.language))
        .filter(_.tag === tag.tag).result.headOption),
        Duration.Inf).isDefined
    } finally db.close()
  }
}
