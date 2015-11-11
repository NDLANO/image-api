/*
 * Part of NDLA Image-API. API for searching and downloading images from NDLA.
 * Copyright (C) 2015 NDLA
 *
 * See LICENSE
 */
package no.ndla.imageapi.business

import no.ndla.imageapi.model.{ImageMetaInformation, ImageMetaSummary}


trait SearchMeta {
  val IndexName: String
  val DocumentName: String
  def matchingQuery(query: Iterable[String], minimumSize:Option[Int], language: Option[String], license: Option[String]): Iterable[ImageMetaSummary]
  def all(minimumSize:Option[Int], license: Option[String]): Iterable[ImageMetaSummary]
  def indexDocument(imageMeta: ImageMetaInformation, indexName: String): Unit
  def indexDocuments(imageMetaList: List[ImageMetaInformation], indexName: Int): Unit
  def createIndex(index: Int): Unit
  def useIndex(index: Int): Unit
  def deleteIndex(index: Int): Unit
  def usedIndex: Int
}
