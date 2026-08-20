package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.entity.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    fun getUserProfile(id: String): Flow<UserProfile?>
    suspend fun updateProfile(user: UserProfile): Result<Unit>
    suspend fun linkPartner(partnerId: String): Result<Unit>
    suspend fun unlinkPartner(): Result<Unit>
}
