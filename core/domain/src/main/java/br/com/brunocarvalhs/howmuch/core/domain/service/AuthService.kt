package br.com.brunocarvalhs.howmuch.core.domain.service

import br.com.brunocarvalhs.howmuch.core.domain.entity.AuthenticatedUser

interface AuthService {
    val currentUser: AuthenticatedUser?
    suspend fun getOrCreateUserId(): AuthenticatedUser
}
