package no.ndla.imageapi.model.domain

case class SearchSettings(
    query: Option[String],
    minimumSize: Option[Int],
    language: Option[String],
    license: Option[String],
    sort: Sort.Value,
    page: Option[Int],
    pageSize: Option[Int],
    includeCopyrighted: Boolean,
    shouldScroll: Boolean,
    modelReleased: Seq[ModelReleasedStatus.Value]
)
