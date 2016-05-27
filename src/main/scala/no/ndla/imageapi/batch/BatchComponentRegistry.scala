package no.ndla.imageapi.batch

import no.ndla.imageapi.batch.integration.CMDataComponent
import no.ndla.imageapi.batch.service.ImportServiceComponent

object BatchComponentRegistry
  extends CMDataComponent
  with ImportServiceComponent
{
  val CMPassword = scala.util.Properties.envOrNone("CM_PASSWORD")
  val CMUser = scala.util.Properties.envOrNone("CM_USER")
  val CMHost = scala.util.Properties.envOrNone("CM_HOST")
  val CMPort = scala.util.Properties.envOrNone("CM_PORT")
  val CMDatabase = scala.util.Properties.envOrNone("CM_DATABASE")

  lazy val cmData = new CMData(CMHost, CMPort, CMDatabase, CMUser, CMPassword)
  lazy val importService = new ImportService
}
