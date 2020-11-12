/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.network.secrets.PropertyKeys
import no.ndla.scalatestsuite.UnitTestSuite

trait UnitSuite extends UnitTestSuite {
  setPropEnv("NDLA_ENVIRONMENT", "local")
  setPropEnv("SEARCH_SERVER", "search-server")
  setPropEnv("SEARCH_REGION", "some-region")
  setPropEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setPropEnv("MIGRATION_HOST", "some-host")
  setPropEnv("MIGRATION_USER", "some-user")
  setPropEnv("MIGRATION_PASSWORD", "some-password")
  setPropEnv("SEARCH_INDEX_NAME", "image-integration-test-index")
  setPropEnv("NDLA_RED_USERNAME", "user")
  setPropEnv("NDLA_RED_PASSWORD", "pass")

  setPropEnv(PropertyKeys.MetaUserNameKey, "username")
  setPropEnv(PropertyKeys.MetaPasswordKey, "secret")
  setPropEnv(PropertyKeys.MetaResourceKey, "resource")
  setPropEnv(PropertyKeys.MetaServerKey, "server")
  setPropEnv(PropertyKeys.MetaPortKey, "1234")
  setPropEnv(PropertyKeys.MetaSchemaKey, "testschema")
}
