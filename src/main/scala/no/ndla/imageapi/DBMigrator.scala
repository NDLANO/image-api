/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi

import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway

object DBMigrator {

  def migrate(datasource: HikariDataSource): Int = {
    val flyway = Flyway
      .configure()
      .table("schema_version") // Flyway's default table name changed, so we specify the old one.
      .dataSource(datasource)
      // Seems like flyway uses datasource.getConnection().getScheme() which is null if the scheme does not exist.
      // Therefore we simply override it with dataSource.getScheme.
      // https://github.com/flyway/flyway/issues/2182
      .schemas(datasource.getSchema)
      .load()
    flyway.migrate()
  }
}
