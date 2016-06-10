package no.ndla.imageapi.integration

import javax.sql.DataSource

trait DataSourceComponent {
  val dataSource: DataSource
}
