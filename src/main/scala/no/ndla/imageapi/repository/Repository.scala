/*
 * Part of NDLA image-api.
 * Copyright (C) 2020 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.repository

import scalikejdbc.{AutoSession, DBSession}

trait Repository[T] {
  def minMaxId(implicit session: DBSession = AutoSession): (Long, Long)
  def documentsWithIdBetween(min: Long, max: Long): Seq[T]
}
