/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import no.ndla.network.secrets.PropertyKeys
import org.scalatest._
import org.scalatest.mockito.MockitoSugar

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with PrivateMethodTester {

  setEnv("NDLA_ENVIRONMENT", "local")
  setEnv(PropertyKeys.MetaUserNameKey, "username")
  setEnv(PropertyKeys.MetaPasswordKey, "secret")
  setEnv(PropertyKeys.MetaResourceKey, "resource")
  setEnv(PropertyKeys.MetaServerKey, "server")
  setEnv(PropertyKeys.MetaPortKey, "1234")
  setEnv(PropertyKeys.MetaSchemaKey, "someschema")
  setEnv("SEARCH_SERVER", "search-server")
  setEnv("SEARCH_REGION", "some-region")
  setEnv("RUN_WITH_SIGNED_SEARCH_REQUESTS", "false")
  setEnv("MIGRATION_HOST", "some-host")
  setEnv("MIGRATION_USER", "some-user")
  setEnv("MIGRATION_PASSWORD", "some-password")
  setEnv("SEARCH_INDEX_NAME", "integration-test-index")


  def setEnv(key: String, value: String) = {
    val field = System.getenv().getClass.getDeclaredField("m")
    field.setAccessible(true)
    val map = field.get(System.getenv()).asInstanceOf[java.util.Map[java.lang.String, java.lang.String]]
    map.put(key, value)
  }
}
