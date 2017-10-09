/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.auth

import no.ndla.imageapi.model.AccessDeniedException
import io.digitallibrary.network.AuthUser



trait User {

  val authUser: AuthUser

  class AuthUser {

    def id(): String = {
      if (AuthUser.get.isEmpty || AuthUser.get.get.isEmpty) {
        throw new AccessDeniedException(("User id required to perform this operation"))
      } else {
        return AuthUser.get.get
      }

    }

  }

}
