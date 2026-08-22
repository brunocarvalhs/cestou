package br.com.brunocarvalhs.howmuch.core.data.repository

import br.com.brunocarvalhs.howmuch.core.data.extensions.toDomain
import br.com.brunocarvalhs.howmuch.core.data.extensions.toModel
import br.com.brunocarvalhs.howmuch.core.data.extensions.toMap
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

    override suspend fun updateProfile(user: UserProfile): Result<Unit> = runCatching {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/${user.id}",
                method = NetworkService.Method.PUT,
                payload = user.toModel().toMap()
            ),
            response = Boolean::class
        )
        Unit
    }

    override suspend fun linkPartner(partnerId: String): Result<Unit> {
        // TODO: Implementar lógica de link real (pode precisar de uma nuvem function ou lógica bidirecional)
        return Result.success(Unit)
    }

    override suspend fun unlinkPartner(): Result<Unit> {
        // TODO: Implementar logic real
        return Result.success(Unit)
    }

    companion object {
        private const val ENDPOINT = "users"
    }
}
