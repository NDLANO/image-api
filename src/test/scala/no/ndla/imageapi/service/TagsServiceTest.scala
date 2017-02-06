/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import java.io.ByteArrayInputStream

import no.ndla.imageapi.model.domain.ImageTag
import no.ndla.imageapi.{TestEnvironment, UnitSuite}

class TagsServiceTest extends UnitSuite with TestEnvironment {
  val service = new TagsService

  test("getISO639 returns a iso639 language code for a valid language url") {
    service.getISO639("http://psi.some.url.org/#nob") should equal(Option("nb"))
    service.getISO639("http://psi.some.url.org/#nno") should equal(Option("nn"))
    service.getISO639("http://psi.some.url.org/#eng") should equal(Option("en"))
  }

  test("getISO639 returns None for an invalid language url") {
    service.getISO639("http://psi.some.url.org/#XXX") should equal(None)
    service.getISO639("http://psi.some.url.org/#YYY") should equal(None)
    service.getISO639("http://psi.some.url.org/#ZZZ") should equal(None)
  }

  test("keywordsJsonToImageTags returns an empty list for an unparsable json body") {
    val jsonString = """{"keyword": [{"""
    service.streamToImageTags(new ByteArrayInputStream(jsonString.getBytes())) should equal(List())
  }

  test("keywordsJsonToImageTags converts a keyword json string to a ImageTag List") {
    val jsonString =
      """{
       "keyword": [{
         "psi": "http://psi.somesite.no/keywords/#keyword-123",
         "topicId": "keyword-123",
         "visibility": "1",
         "approved": "true",
         "approval_date": "2014-06-27 08:20:05.294",
         "processState": "2",
         "psis": ["http://psi.somesite.no/keywords/#keyword-123"],
         "originatingSites": ["http://somesite.org/"],
         "types": [{
           "typeId": "keyword",
           "names": [
             {"http://psi.somesite.org/#language-neutral": "Keyword"},
             {"http://psi.somesite.org/iso/639/#eng": "Keyword"},
             {"http://psi.somesite.org/iso/639/#nob": "Nøkkelord"}
           ]
         }],
         "names": [
           {
           "wordclass": "noun",
           "data": [
             {"http://psi.oasis-open.org/iso/639/#nno": "folkevise"},
             {"http://psi.topic.ndla.no/#language-neutral": "folk song"},
             {"http://psi.oasis-open.org/iso/639/#eng": "folk song"},
             {"http://psi.oasis-open.org/iso/639/#nob": "folkevise"}
           ]}
         ]
       }]}"""
    val expectedResult = List(
      ImageTag(List("folkevise"), Some("nn")),
      ImageTag(List("folkevise"), Some("nb")),
      ImageTag(List("folk song"), None),
      ImageTag(List("folk song"), Some("en"))
    )
    service.streamToImageTags(new ByteArrayInputStream(jsonString.getBytes())) should equal(expectedResult)
  }
}
