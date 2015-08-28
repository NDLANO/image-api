package no.ndla.imageapi.business

import no.ndla.imageapi.model.{ImageMetaSummary, ImageMetaInformation}

trait ImageMeta {

  def all(minimumSize:Option[Int], license: Option[String]): Iterable[ImageMetaSummary]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tags: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String]): Iterable[ImageMetaSummary]

  def containsExternalId(externalId: String): Boolean
  def insert(imageMetaInformation: ImageMetaInformation, externalId: String)
  def update(imageMetaInformation: ImageMetaInformation, externalId: String)

}
