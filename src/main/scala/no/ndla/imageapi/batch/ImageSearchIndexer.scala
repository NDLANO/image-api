package no.ndla.imageapi.batch

import no.ndla.imageapi.integration.AmazonIntegration

object ImageSearchIndexer {

  val dbMeta = AmazonIntegration.getImageMeta()
  val searchMeta = AmazonIntegration.getSearchMeta()

  def main(args: Array[String]) {
    val start = System.currentTimeMillis()

    searchMeta.createIndex()
    dbMeta.foreach(imageMeta => {
      searchMeta.indexDocument(imageMeta)
      println(s"Indexed document with id: ${imageMeta.id}.")
    })

    println(s"Indexing took ${System.currentTimeMillis() - start} ms.")
  }
}

