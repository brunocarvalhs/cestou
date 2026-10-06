package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.core.data.model.UserProfileModel
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val networkService: NetworkService
) : UserRepository {

    override fun getUserProfile(id: String): Flow<UserProfile?> {
        return networkService.observe<UserProfileModel>(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$id",
                method = NetworkService.Method.GET
            )
        ).map { it?.toDomain() }
    }

    // POST with an id maps to set() (create-or-replace); PUT maps to update(), which fails on a
    // missing document. The e-mail is left out on purpose: users/{uid} is readable by any
    // signed-in user who knows the uid (AD-009).
    override suspend fun saveProfile(user: UserProfile): Result<Unit> = runCatching {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.POST,
                payload = mapOf("id" to user.id, "name" to user.name, "photoUrl" to user.photoUrl)
            ),
            response = Boolean::class
        )
        Unit
    }

    override suspend fun deleteProfile(id: String): Result<Unit> = runCatching {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$id",
                method = NetworkService.Method.DELETE
            ),
            response = Boolean::class
        )
        Unit
    }

    companion object {
        private const val ENDPOINT = "users"
    }
}
