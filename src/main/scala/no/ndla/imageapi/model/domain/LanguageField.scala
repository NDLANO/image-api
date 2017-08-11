package no.ndla.imageapi.model.domain


trait LanguageField[T] {
  def value: T
  def language: String
}
