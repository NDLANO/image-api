package no.ndla.imageapi.business

import java.io.InputStream

import model.Image

trait ImageBucket {
  def get(imageKey: String): Option[(String, InputStream)]
  def upload(image: Image, imageDirectory: String)
  def contains(image: Image): Boolean
  def exists(): Boolean
  def create()
}
