package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val authState: Flow<AuthenticatedUser?>
    val currentUser: AuthenticatedUser?
    suspend fun getOrCreateUserId(): AuthenticatedUser
    suspend fun signInAnonymously(): Result<AuthenticatedUser>
    suspend fun signOut(): Result<Unit>
    suspend fun signInWithGoogle(): Result<AuthenticatedUser>
    suspend fun signInWithApple(): Result<AuthenticatedUser>
}
