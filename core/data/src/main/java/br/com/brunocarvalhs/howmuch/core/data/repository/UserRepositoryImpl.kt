package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.core.data.model.UserProfileModel
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.deleteDocumentWithSubCollections
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
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
            .catch { e ->
                Timber.e(e, "Error observing user profile for id: %s", id)
                emit(null)
            }
    }

    // POST with an id maps to set() (create-or-replace); PUT maps to update(), which fails on a
    // missing document. The e-mail is left out on purpose: users/{uid} is readable by any
    // signed-in user who knows the uid (AD-009).
    //
    // FirestoreClient.post() always returns the document id as a String (never a Boolean), so
    // `response` must match that real return type. Declaring `Boolean::class` here made every
    // saveProfile() call try (and fail) to decode a Firebase UID string as a boolean, logging a
    // non-fatal SerializationException on every save even though the result is discarded below.
    override suspend fun saveProfile(user: UserProfile): Result<Unit> = runCatching {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.POST,
                payload = mapOf("id" to user.id, "name" to user.name, "photoUrl" to user.photoUrl)
            ),
            response = String::class
        )
        Unit
    }

    override suspend fun deleteProfile(id: String): Result<Unit> = runCatching {
        networkService.deleteDocumentWithSubCollections("$ENDPOINT/$id", "common-products")
    }

    companion object {
        private const val ENDPOINT = "users"
    }
}
