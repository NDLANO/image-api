package no.ndla.imageapi.business

import no.ndla.imageapi.model.ImageMetaInformation

import scala.util.Try

trait IndexMeta {
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: String): Unit
  def createIndex(indexName: String): Unit
  def updateAliasTarget(newIndexName: String, oldIndexName: Option[String]): Unit
  def deleteIndex(indexName: String): Unit
  def aliasTarget: Option[String]
}
