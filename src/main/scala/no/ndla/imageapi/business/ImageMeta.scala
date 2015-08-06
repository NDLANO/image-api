package no.ndla.imageapi.business

import model.ImageMetaInformation

trait ImageMeta {

  def all(): List[ImageMetaInformation]
  def withId(id: String): Option[ImageMetaInformation]
  def withTags(tag: String): List[ImageMetaInformation]

  def upload(imageMetaInformation: ImageMetaInformation)

  def exists(): Boolean
  def create()

}
