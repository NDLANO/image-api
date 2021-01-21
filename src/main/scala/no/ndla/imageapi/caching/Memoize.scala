/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.caching

class Memoize[R](f: () => R, maxAgeMs: Long) extends (() => R) {
  case class CacheValue(value: R, lastUpdated: Long) {
    def isExpired: Boolean = lastUpdated + maxAgeMs <= System.currentTimeMillis()
  }

  private[this] var cache: Option[CacheValue] = None

  def apply(): R = {
    cache match {
      case Some(cachedValue) if !cachedValue.isExpired => cachedValue.value
      case _ => {
        cache = Some(CacheValue(f(), System.currentTimeMillis()))
        cache.get.value
      }
    }
  }
}

object Memoize {
  def apply[R](maxAgeMs: Long, f: () => R) = new Memoize(f, maxAgeMs)
}
