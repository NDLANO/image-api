/*
 * Part of NDLA image-api
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.model.domain

trait LanguageField[T] {
  def value: T
  def language: String
}
