package br.com.brunocarvalhs.howmuch.core.domain.services

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import kotlinx.coroutines.flow.Flow

interface AuthService {
    val authState: Flow<AuthenticatedUser?>
    val currentUser: AuthenticatedUser?
    suspend fun getOrCreateUserId(): AuthenticatedUser
    suspend fun signOut(): Result<Unit>
    suspend fun updateUserId(userId: String)
    /**
     * Fails with [br.com.brunocarvalhs.howmuch.core.domain.exception.RecentLoginRequiredException]
     * when Firebase demands a fresh sign-in first.
     */
    suspend fun deleteAccount(): Result<Unit>

    /** True when the last sign-in is recent enough for sensitive operations such as [deleteAccount]. */
    fun hasRecentLogin(): Boolean

    /** Sets the signed-in user's display name and publishes it on [authState] right away. */
    suspend fun updateDisplayName(name: String): Result<Unit>

    /**
     * Signs this device in with a Google ID token minted on another device (the paired phone).
     * Fails, and stays signed out, when the token resolves to an account other than [expectedUserId].
     */
    suspend fun signInWithGoogleIdToken(idToken: String, expectedUserId: String): Result<Unit>
}
