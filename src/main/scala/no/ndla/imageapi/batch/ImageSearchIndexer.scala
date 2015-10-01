package no.ndla.imageapi.batch

import no.ndla.imageapi.integration.AmazonIntegration

object ImageSearchIndexer {

  val meta = AmazonIntegration.getImageMeta()
  val search = AmazonIntegration.getSearchMeta()

  def main(args: Array[String]) {
    val start = System.currentTimeMillis()

    search.createIndex()
    meta.foreach(imageMeta => {
      search.indexDocument(imageMeta)
      println(s"Indexed document with id: ${imageMeta.id}.")
    })

    println(s"Indexing took ${System.currentTimeMillis() - start} ms.")
  }
}

