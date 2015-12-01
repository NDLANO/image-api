package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

import scala.util.Try

trait IndexMeta {
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: String): Unit
  def createIndex(indexName: String): Unit
  def useIndex(indexName: String): Unit
  def deleteIndex(indexName: String): Unit
  def indexInUse: Option[String]
}
