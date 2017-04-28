/*
 * Part of NDLA image_api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service

import no.ndla.imageapi.model.AccessDeniedException
import no.ndla.network.AuthUser

trait AuthenticationRole {

  val authRole: AuthRole

  class AuthRole {
    def assertHasRole(role: String): Unit = {
      if (!AuthUser.hasRole(role))
        throw new AccessDeniedException("User is missing required role to perform this operation")
    }
  }

}

trait AuthenticationUser {

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
