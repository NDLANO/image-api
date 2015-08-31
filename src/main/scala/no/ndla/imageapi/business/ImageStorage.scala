/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.business

import java.io.InputStream

import no.ndla.imageapi.model.{Image, ImageMetaInformation}

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

  /**
   * Uploads an image from the given url to storage.
   * 
   * @param image Metadata for the image.
   * @param storageKey The name used in storage
   * @param urlOfImage The url of the source
   */
  def uploadFromUrl(image: Image, storageKey:String, urlOfImage: String)

  /**
   * Checks if a key already exists in the imagestorage.
   *
   * @param storageKey The key to check for existence.
   * @return True if storageKey exists in storage. False otherwise
   */
  def contains(storageKey: String): Boolean

  def exists(): Boolean
  def create()
}
