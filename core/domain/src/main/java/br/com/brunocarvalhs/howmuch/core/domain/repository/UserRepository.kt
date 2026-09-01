package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(id: String): Flow<UserProfile?>
    suspend fun updateProfile(user: UserProfile): Result<Unit>
    suspend fun deleteProfile(id: String): Result<Unit>
}
