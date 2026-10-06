package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(id: String): Flow<UserProfile?>
    /** Creates or replaces `users/{id}` with id, name and photo. The e-mail is never stored (AD-009). */
    suspend fun saveProfile(user: UserProfile): Result<Unit>
    suspend fun deleteProfile(id: String): Result<Unit>
}
