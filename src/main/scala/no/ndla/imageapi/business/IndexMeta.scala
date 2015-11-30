package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

trait IndexMeta {
  def indexDocument(imageMeta: ImageMetaInformation, indexName: Int): Unit
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: Int): Unit
  def createIndex(index: Int): Unit
  def useIndex(index: Int): Unit
  def deleteIndex(index: Int): Unit
  def usedIndex: Int
}
