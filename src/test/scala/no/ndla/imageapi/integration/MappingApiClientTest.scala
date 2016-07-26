/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import no.ndla.imageapi.model.domain.License
import no.ndla.imageapi.{TestEnvironment, UnitSuite}
import no.ndla.network.model.HttpRequestException
import org.mockito.Mockito._
import org.mockito.Matchers._

import scala.util.{Failure, Success}
import scalaj.http.HttpRequest

class MappingApiClientTest extends UnitSuite with TestEnvironment {

  var client = new MappingApiClient
  val sampleLicenses = Seq(LicenseDefinition("by", "Creative Commons Attribution 2.0 Generic", Some("https://creativecommons.org/licenses/by/2.0/")),
    LicenseDefinition("by-sa", "Creative Commons Attribution-ShareAlike 2.0 Generic", Some("https://creativecommons.org/licenses/by-sa/2.0/")),
    LicenseDefinition("by-nc", "Creative Commons Attribution-NonCommercial 2.0 Generic", Some("https://creativecommons.org/licenses/by-nc/2.0/")))

  val sampleMappings = Map(
    "nob" -> "nb",
    "eng" -> "en",
    "fra" -> "fr"
  )

  override def beforeEach() {
    client = new MappingApiClient
  }

  test("That getLicenseDefinition returns a license if found") {
    val expectedResult = Some(License("by", "Creative Commons Attribution 2.0 Generic", Some("https://creativecommons.org/licenses/by/2.0/")))
    when(ndlaClient.fetch[Seq[LicenseDefinition]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Seq[LicenseDefinition]]])).thenReturn(Success(sampleLicenses))
    client.getLicenseDefinition("by") should equal(expectedResult)
  }

  test("That getLicenseDefinition returns None if license is not found") {
    client.getLicenseDefinition("garbage") should equal(None)
  }

  test("That getLicenseDefinition throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Seq[LicenseDefinition]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Seq[LicenseDefinition]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.getLicenseDefinition("garbage")
    }
  }

  test("That get6391CodeFor6392Code returns a language code if it exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.get6391CodeFor6392Code("nob") should equal (Some("nb"))
  }

  test("That get6391CodeFor6392Code returns None if the language code does not exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.get6391CodeFor6392Code("garbage") should equal (None)
  }

  test("That get6391CodeFor6392Code throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.getLicenseDefinition("garbage")
    }
  }

  test("That languageCodeSupported returns true if it exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported("nob") should equal (true)
  }

  test("That languageCodeSupported returns false if the language code does not exists") {
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Success(sampleMappings))
    client.languageCodeSupported("garbage") should equal (false)
  }

  test("That languageCodeSupported throws an exception if the http call failed") {
    val exception = mock[HttpRequestException]
    when(exception.is404).thenReturn(true)
    when(ndlaClient.fetch[Map[String, String]](any[HttpRequest], any[Option[String]], any[Option[String]])(any[Manifest[Map[String, String]]])).thenReturn(Failure(exception))

    intercept[HttpRequestException] {
      client.languageCodeSupported("garbage")
    }
  }

}
