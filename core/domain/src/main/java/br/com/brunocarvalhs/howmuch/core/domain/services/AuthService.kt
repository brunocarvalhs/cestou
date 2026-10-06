package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val authState: Flow<AuthenticatedUser?>
    val currentUser: AuthenticatedUser?
    suspend fun getOrCreateUserId(): AuthenticatedUser
    suspend fun signOut(): Result<Unit>
    suspend fun updateUserId(userId: String)
    suspend fun deleteAccount(): Result<Unit>

    /** Sets the signed-in user's display name and publishes it on [authState] right away. */
    suspend fun updateDisplayName(name: String): Result<Unit>
}
