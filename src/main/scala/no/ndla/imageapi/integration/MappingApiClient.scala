/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.integration

import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.caching.Memoize
import no.ndla.imageapi.model.domain.License
import no.ndla.network.NdlaClient

import scala.util.{Failure, Success}
import scalaj.http.Http

trait MappingApiClient {
  this: NdlaClient =>
  val mappingApiClient: MappingApiClient

  class MappingApiClient {

    val allLanguageMappingsEndpoint = s"http://${ImageApiProperties.MappingHost}/iso639"
    val allLicenseDefinitionsEndpoint = s"http://${ImageApiProperties.MappingHost}/licenses"

    def getLicenseDefinition(licenseName: String): Option[License] = {
      getLicenseDefinitions().find(_.license == licenseName).map(l => License(l.license, l.description, l.url))
    }

    def get6391CodeFor6392Code(languageCode6392: String): Option[String] = getLanguageMapping().find(_._1 == languageCode6392).map(_._2)

    def languageCodeSupported(languageCode: String): Boolean = getLanguageMapping().exists(_._1 == languageCode)

    private val getLicenseDefinitions = Memoize[Seq[LicenseDefinition]](ImageApiProperties.LicenseMappingCacheAgeInMs, () => {
      ndlaClient.fetch[Seq[LicenseDefinition]](Http(allLicenseDefinitionsEndpoint)) match {
        case Success(definitions) => definitions
        case Failure(ex) => throw ex
      }
    })

    private val getLanguageMapping = Memoize[Map[String, String]](ImageApiProperties.IsoMappingCacheAgeInMs, () => {
      ndlaClient.fetch[Map[String, String]](Http(allLanguageMappingsEndpoint)) match {
        case Success(map) => map
        case Failure(ex) => throw ex
      }
    })
  }
}

case class LicenseDefinition(license: String, description: String, url: Option[String])
