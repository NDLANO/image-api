/*
 * Part of NDLA image-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package db.migration

import com.amazonaws.services.s3.model.AmazonS3Exception
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}

import java.awt.image.BufferedImage
import scala.util.{Failure, Success}

class V12__AddImageMetadataTest extends UnitSuite with TestEnvironment {
  val migration = spy(new V12__AddImageMetadata)

  val testUrl = "http://test.test/1"
  val imageStreamMock = mock[ImageStream]
  val bufferedImageMock = mock[BufferedImage]

  test("migration should update to new status format") {

    doReturn(Success(imageStreamMock)).when(imageStorage).get(any[String])
    when(migration.get(testUrl)).thenReturn(Success(bufferedImageMock))
    when(bufferedImageMock.getWidth()).thenReturn(100)
    when(bufferedImageMock.getHeight()).thenReturn(200)

    val old =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expected =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"width":100,"height":200}"""
    migration.convertImageUpdate(old) should equal(expected)
  }

  test("migration not do anything if the document already has new status format") {

    doReturn(Success(imageStreamMock)).when(imageStorage).get(any[String])
    when(migration.get(testUrl)).thenReturn(Success(bufferedImageMock))
    when(bufferedImageMock.getWidth()).thenReturn(100)
    when(bufferedImageMock.getHeight()).thenReturn(200)
    val original =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"width":100,"height":200}"""

    migration.convertImageUpdate(original) should equal(original)
  }

  test("migration sets width and height to 0 if image does not exist in s3") {

    doReturn(Success(imageStreamMock)).when(imageStorage).get(any[String])
    when(migration.get(testUrl)).thenReturn(Failure(new Exception("no such key")))

    val old =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"]}"""
    val expected =
      s"""{"id":"1","metaUrl":"$testUrl","title":{"title":"Elg i busk","language":"nb"},"created":"2017-04-01T12:15:32Z","createdBy":"ndla124","modelRelease":"yes","alttext":{"alttext":"Elg i busk","language":"nb"},"imageUrl":"$testUrl","size":2865539,"contentType":"image/jpeg","copyright":{"license":{"license":"gnu","description":"gnuggert","url":"https://gnuli/"},"agreementId":1,"origin":"http://www.scanpix.no","creators":[{"type":"Forfatter","name":"Knutulf Knagsen"}],"processors":[{"type":"Redaksjonelt","name":"Kåre Knegg"}],"rightsholders":[]},"tags":{"tags":["rovdyr","elg"],"language":"nb"},"caption":{"caption":"Elg i busk","language":"nb"},"supportedLanguages":["nb"],"width":0,"height":0}"""
    migration.convertImageUpdate(old) should equal(expected)
  }
}
