package no.ndla.imageapi.business

import java.io.InputStream

import model.ImageMetaInformation

trait ImageStorage {
  /**
   * Retrieves a handle to the image from storage.
   *
   * @param imageKey The key name the image is stored as
   * @return Optionally a tuple with mimetype and inputstream to the actual image
   */
  def get(imageKey: String): Option[(String, InputStream)]

  /**
   * Uploads an image to storage.
   * Example usage:
   * - The image is located at /home/user/images/myImage.jpg
   * - ImageMetaInformation holds myImage.jpg
   * - imageDirectory should be set to /home/user/images/
   *
   * TODO: This method should be rewritten
   *
   * @param imageMetaInformation The meta-information describing the image.
   * @param imageDirectory The path (on the local host) to where the image resides, excluding the image name.
   */
  def upload(imageMetaInformation: ImageMetaInformation, imageDirectory: String)

  def contains(imageMetaInformation: ImageMetaInformation): Boolean
  def exists(): Boolean
  def create()
}
