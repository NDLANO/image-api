package no.ndla.imageapi.business

import no.ndla.imageapi.model.{ImageMetaSummary, ImageMetaInformation}

trait ImageMeta {

  def all(minimumSize:Option[Int]): Iterable[ImageMetaSummary]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tags: Iterable[String], minimumSize:Option[Int], language: Option[String]): Iterable[ImageMetaSummary]

  def containsExternalId(externalId: String): Boolean
  def upload(imageMetaInformation: ImageMetaInformation, externalId: String)

}
