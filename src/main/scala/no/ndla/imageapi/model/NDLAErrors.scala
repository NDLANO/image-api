/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.model

import com.sksamuel.elastic4s.http.RequestFailure

class ImageNotFoundException(message: String) extends RuntimeException(message)

class AccessDeniedException(message: String) extends RuntimeException(message)

class ImportException(message: String) extends RuntimeException(message)

case class ValidationException(message: String = "Validation error", val errors: Seq[ValidationMessage])
    extends RuntimeException(message)

object ValidationException {
  def apply(path: String, msg: String) = new ValidationException(errors = Seq(ValidationMessage(path, msg)))
}

case class InvalidUrlException(message: String) extends RuntimeException(message)

case class ValidationMessage(field: String, message: String)

case class NdlaSearchException(rf: RequestFailure)
    extends RuntimeException(
      s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
    )
class ResultWindowTooLargeException(message: String) extends RuntimeException(message)
case class ElasticIndexingException(message: String) extends RuntimeException(message)

class ImageStorageException(message: String) extends RuntimeException(message)
