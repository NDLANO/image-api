package no.ndla.imageapi.business

import java.io.InputStream

import model.ImageMetaInformation

trait ImageStorage {
  def get(imageKey: String): Option[(String, InputStream)]
  def upload(imageMetaInformation: ImageMetaInformation, imageDirectory: String)
  def contains(imageMetaInformation: ImageMetaInformation): Boolean
  def exists(): Boolean
  def create()
}
