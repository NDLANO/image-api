package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

trait IndexMeta {
  def indexDocument(imageMeta: ImageMetaInformation, indexName: String): Unit
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: String): Unit
  def createIndex(index: String): Unit
  def useIndex(index: String): Either[String, String]
  def deleteIndex(index: String): Either[String, String]
  def usedIndex: Option[String]
}
