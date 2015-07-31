package no.ndla.imageapi.business

import model.Image

trait ImageMeta {

  def all(): List[Image]
  def withId(id: String): Option[Image]
  def withTags(tag: String): List[Image]

  def upload(image: Image)

  def exists(): Boolean
  def create()

}
