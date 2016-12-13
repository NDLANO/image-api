/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

object IntegrationTest extends Tag("no.ndla.IntegrationTest")

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with PrivateMethodTester {

  ImageApiProperties.setProperties(Map(
    "NDLA_ENVIRONMENT" -> Some("unittest"),
    "STORAGE_NAME" -> Some("TestBucket"),
    "STORAGE_ACCESS_KEY" -> Some("AccessKey"),
    "STORAGE_SECRET_KEY" -> Some("SecretKey"),
    "CONTACT_EMAIL" -> Some("someone@somewhere"),
    "META_USER_NAME" -> Some("username"),
    "META_PASSWORD" -> Some("secrets"),
    "META_RESOURCE" -> Some("resource"),
    "META_SERVER" -> scala.util.Properties.envOrNone("DOCKER_ADDR"),
    "META_PORT" -> Some("5432"),
    "META_SCHEMA" -> Some("someschema"),
    "META_INITIAL_CONNECTIONS" -> Some("3"),
    "META_MAX_CONNECTIONS" -> Some("20"),
    "SEARCH_ENGINE_ENV_TCP_PORT" -> Some("123"),
    "SEARCH_ENGINE_ENV_CLUSTER_NAME" -> Some("search-engine"),
    "SEARCH_INDEX" -> Some("images"),
    "SEARCH_DOCUMENT" -> Some("image"),
    "INDEX_BULK_SIZE" -> Some("1000"),
    "HOST_ADDR" -> Some("host-addr"),
    "DOMAIN" -> Some("http://somedomain"),
    "NDLACOMPONENT" -> Some("image-api"),
    "MIGRATION_HOST" -> Some("image-api"),
    "MIGRATION_USER" -> Some("user"),
    "MIGRATION_PASSWORD" -> Some("password"),
    "SEARCH_SERVER" -> Some("search-server"),
    "RUN_WITH_SIGNED_SEARCH_REQUESTS" -> Some("false"),
    "SEARCH_REGION" -> Some("eu-central-1")
  ))
}
