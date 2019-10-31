/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.domain.{Author, ImageMetaInformation, ImageTag}
import no.ndla.imageapi.{TestData, TestEnvironment, UnitSuite}
import no.ndla.imageapi.model.{ImportException, ImageStorageException, domain}
import no.ndla.mapping._
import no.ndla.mapping.License.{CC0, CC_BY_SA, PublicDomain}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import no.ndla.imageapi.integration

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest

class ImportServiceTest extends UnitSuite with TestEnvironment {
  override val importService = new ImportService

  override def beforeEach = {
    reset(imageRepository, tagsService, migrationApiClient, imageStorage, indexBuilderService)
  }

  test("import should fail if call to migration-api fails") {
    when(migrationApiClient.getMetaDataForImage("1234"))
      .thenReturn(Failure(new RuntimeException("No image with that id")))
    val result = importService.importImage("1234")
    result.isFailure should be(true)
    result.failed.get.getMessage.contains(s"No image with that id") should be(true)
  }

  test("import should fail if raw image could not be uploaded") {
    when(migrationApiClient.getMetaDataForImage("1234")).thenReturn(Success(TestData.migrationImageElg))
    when(imageStorage.uploadFromUrl(any[domain.Image], any[String], any[HttpRequest]))
      .thenReturn(Failure(new RuntimeException("upload failed")))

    val result = importService.importImage("1234")
    result.isFailure should be(true)
    result.failed.get.isInstanceOf[ImageStorageException] should be(true)
    result.failed.get.getMessage should equal(
      s"Upload of image '/${TestData.migrationImageElg.mainImage.originalFile}' to S3 failed.: upload failed")
  }

  test("import should fail if failed to update database") {
    when(migrationApiClient.getMetaDataForImage("1234")).thenReturn(Success(TestData.migrationImageElg))
    when(imageStorage.uploadFromUrl(any[domain.Image], any[String], any[HttpRequest])).thenReturn(Success("ok"))
    when(tagsService.forImage("1234")).thenReturn(Success(List.empty))
    when(imageRepository.withExternalId("1234")).thenReturn(None)
    when(imageRepository.insertWithExternalId(any[ImageMetaInformation], any[String]))
      .thenThrow(new RuntimeException("Not today, son"))

    val result = importService.importImage("1234")
    result.isFailure should be(true)
    result.failed.get.getMessage should equal(s"Not today, son")
  }

  test("import should return Success if everything went fine") {
    when(migrationApiClient.getMetaDataForImage("1234")).thenReturn(Success(TestData.migrationImageElg))
    when(imageStorage.uploadFromUrl(any[domain.Image], any[String], any[HttpRequest])).thenReturn(Success("ok"))
    when(tagsService.forImage("1234")).thenReturn(Success(List.empty))
    when(imageRepository.withExternalId("1234")).thenReturn(None)
    when(imageRepository.insertWithExternalId(any[ImageMetaInformation], any[String])).thenReturn(TestData.elg)
    when(indexBuilderService.indexDocument(any[domain.ImageMetaInformation])).thenReturn(Success(TestData.elg))

    val Success(result) = importService.importImage("1234")
    result should equal(TestData.elg)

    verify(imageRepository, times(1)).insertWithExternalId(any[domain.ImageMetaInformation], any[String])
    verify(indexBuilderService, times(1)).indexDocument(any[domain.ImageMetaInformation])
  }

  test("uploadRawImage should return a domain.Image where the filename is url-encoded") {
    val imageMeta = TestData.migrationImageMeta.copy(originalFile = "oh my god, Becky, look at her butt.jpg")
    when(imageStorage.objectExists(any[String])).thenReturn(true)
    when(imageStorage.objectSize(any[String])).thenReturn(imageMeta.originalSize.toInt)

    val Success(result) = importService.uploadRawImage(imageMeta)
    result should equal(
      domain.Image("/oh%20my%20god,%20Becky,%20look%20at%20her%20butt.jpg",
                   imageMeta.originalSize.toInt,
                   imageMeta.originalMime))
  }

  test("That oldToNewLicenseKey throws on invalid license") {
    assertThrows[ImportException] {
      importService.oldToNewLicenseKey("publicdomain")
    }
  }

  test("That oldToNewLicenseKey converts correctly") {
    val cc0 = License.getLicense(CC0.toString)
    val pd = License.getLicense(PublicDomain.toString)
    importService.oldToNewLicenseKey("nolaw") should be(cc0)
    importService.oldToNewLicenseKey("noc") should be(pd)
  }

  test("That oldToNewLicenseKey does not convert an license that should not be converted") {
    val bySa = License.getLicense(CC_BY_SA.toString)
    importService.oldToNewLicenseKey("by-sa") should be(bySa)
  }

  test("That authors are translated correctly") {
    val authors = List(
      integration.ImageAuthor("Opphavsmann", "A"),
      integration.ImageAuthor("Redaksjonelt", "B"),
      integration.ImageAuthor("redaKsJoNelT", "C"),
      integration.ImageAuthor("distributør", "D"),
      integration.ImageAuthor("leVerandør", "E"),
      integration.ImageAuthor("Språklig", "F")
    )
    val meta = integration.MainImageImport(integration.ImageMeta("5", "12", "nb", "a", None, "", "", "", "", None),
                                           authors,
                                           Some("by-sa"),
                                           None,
                                           List())

    val copyright = importService.toDomainCopyright(meta)
    copyright.creators should contain(Author("Originator", "A"))

    copyright.rightsholders should contain(Author("Distributor", "D"))
    copyright.rightsholders should contain(Author("Supplier", "E"))

    copyright.processors should contain(Author("Linguistic", "F"))
    copyright.processors should contain(Author("Editorial", "B"))
    copyright.processors should contain(Author("Editorial", "C"))

  }

  test("That only tags with relevant languages are stored") {
    val nbTags = ImageTag(Seq("hei", "norge", "knekkebrød"), "nb")
    val nbTags2 = ImageTag(Seq("brunost", "også"), "nb")
    val nbMerged = ImageTag(nbTags.tags ++ nbTags2.tags, "nb")
    val enTags = ImageTag(Seq("hello", "englang", "chips"), "en")
    val zhTags = ImageTag(Seq("我不懂中文", "亨里克"), "zh")

    when(tagsService.forImage("1")).thenReturn(Success(List(nbTags, enTags)))
    when(tagsService.forImage("2")).thenReturn(Success(List(nbTags2, zhTags)))

    val image = domain.Image("/123.png", 200, "image/png")
    val nbMeta = integration.ImageMeta("1", "1", "nb", "a", None, "", "", "", "", None)
    val zhMeta = integration.ImageMeta("2", "1", "zh", "a", None, "", "", "", "", None)

    val meta = integration.MainImageImport(nbMeta, List.empty, Some("by-sa"), None, List(zhMeta))

    val result = importService.toDomainImage(meta, image)

    result.tags should be(Seq(zhTags, nbMerged))
  }

}
