package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

/**
  * Created by thomas on 11.11.15.
  */
trait IndexAdmin {
  def indexDocument(imageMeta: ImageMetaInformation, indexName: String): Unit
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: Int): Unit
  def createIndex(index: Int): Unit
  def useIndex(index: Int): Unit
  def deleteIndex(index: Int): Unit
  def usedIndex: Int
}
