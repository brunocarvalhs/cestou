package br.com.brunocarvalhs.howmuch.feature.shopping.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.service.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.service.make
import br.com.brunocarvalhs.howmuch.core.domain.service.observe
import br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper.toDomain
import br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper.toModel
import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShoppingModel
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val networkService: NetworkService,
    private val authService: AuthService
) : ShoppingRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun observeAll(): Flow<List<Shopping>> = flow {
        val user = authService.getOrCreateUserId()
        networkService.observe<List<ShoppingModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("users" to user.id)
            )
        ).collect { models ->
            emit(models?.map { it.toDomain() } ?: emptyList())
        }
    }

    override fun observeById(id: String): Flow<Shopping?> = flow {
        networkService.observe<ShoppingModel>(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$id",
                method = NetworkService.Method.GET,
            )
        ).collect { model ->
            emit(model?.toDomain())
        }
    }

    override suspend fun getAll(): List<Shopping> = withContext(ioDispatcher) {
        val user = authService.getOrCreateUserId()
        val response = networkService.make<List<ShoppingModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("users" to user.id)
            )
        )
        return@withContext response?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getById(id: String): Shopping? = withContext(ioDispatcher) {
        val response = networkService.make<ShoppingModel>(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$id",
                method = NetworkService.Method.GET,
            )
        )
        return@withContext response?.toDomain()
    }

    override suspend fun create(shopping: Shopping): Unit = withContext(ioDispatcher) {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.POST,
                payload = shopping.toModel().toMap()
            ),
            response = String::class
        )
    }

    override suspend fun update(shopping: Shopping): Unit = withContext(ioDispatcher) {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/${shopping.id}",
                method = NetworkService.Method.PUT,
                payload = shopping.toModel().toMap()
            ),
            response = Boolean::class
        )
    }

    override suspend fun delete(shopping: Shopping): Unit = withContext(ioDispatcher) {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/${shopping.id}",
                method = NetworkService.Method.DELETE,
            ),
            response = Boolean::class
        )
    }

    override suspend fun join(shoppingId: String, userId: String): Unit = withContext(ioDispatcher) {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$shoppingId",
                method = NetworkService.Method.PUT,
                payload = mapOf("users" to listOf(userId))
            ),
            response = Boolean::class
        )
    }

    override suspend fun getByShortCode(shortCode: String): Shopping? = withContext(ioDispatcher) {
        val response = networkService.make<List<ShoppingModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("shortCode" to shortCode)
            )
        )?.firstOrNull()
        return@withContext response?.toDomain()
    }

    override suspend fun updatePositions(shoppings: List<Shopping>): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            // Firestore updates would be needed here for each shopping's position
            // For now, we assume success or implement a batch update if needed
            Unit
        }
    }

    companion object {
        private const val ENDPOINT = "shopping"
    }
}
