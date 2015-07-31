package no.ndla.imageapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

object IntegrationTest extends Tag("no.ndla.IntegrationTest")

abstract class UnitSpec extends FlatSpec with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach
