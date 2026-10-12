package br.com.brunocarvalhs.howmuch.feature.shopping.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.deleteDocumentWithSubCollections
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.core.domain.services.observe
import br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper.toDomain
import br.com.brunocarvalhs.howmuch.feature.shopping.data.mapper.toModel
import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShoppingModel
import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShortCodeModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// One operation per ShoppingRepository method; see the interface.
@Suppress("TooManyFunctions")
@Singleton
class ShoppingRepositoryImpl @Inject constructor(
    private val networkService: NetworkService,
    private val authService: AuthService
) : ShoppingRepository {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    override fun observeAll(): Flow<List<Shopping>> = flow {
        val userId = runCatching { authService.getOrCreateUserId().id }.getOrNull()
        if (userId == null) {
            emit(emptyList())
            return@flow
        }
        networkService.observe<List<ShoppingModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("users" to userId)
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
        val userId = runCatching { authService.getOrCreateUserId().id }.getOrNull()
            ?: return@withContext emptyList()
        val response = networkService.make<List<ShoppingModel>>(
            request = NetworkService.NetworkRequest(
                endpoint = ENDPOINT,
                method = NetworkService.Method.GET,
                query = mapOf("users" to userId)
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
        writeShortCodeIndex(shopping)
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
        // Index first (its delete rule checks the list, which must still exist), and only when it points
        // here: legacy lists have none, and duplicated lists copy the original's code.
        shopping.shortCode?.let { code ->
            val index = networkService.make<ShortCodeModel>(
                NetworkService.NetworkRequest(endpoint = "$SHORT_CODES/$code", method = NetworkService.Method.GET)
            )
            if (index?.shoppingId == shopping.id) {
                networkService.make<Boolean>(
                    NetworkService.NetworkRequest(
                        endpoint = "$SHORT_CODES/$code",
                        method = NetworkService.Method.DELETE
                    )
                ) ?: throw IllegalStateException("Failed to delete share code $code")
            }
        }
        networkService.deleteDocumentWithSubCollections("$ENDPOINT/${shopping.id}", "products")
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

    // make() swallows errors and returns null, so a null result here means the write failed.
    override suspend fun leave(shoppingId: String, userId: String): Unit = withContext(ioDispatcher) {
        networkService.make(
            request = NetworkService.NetworkRequest(
                endpoint = "$ENDPOINT/$shoppingId",
                method = NetworkService.Method.PUT,
                payload = mapOf(
                    "users" to NetworkService.FieldOp.ArrayRemove(listOf(userId)),
                    "roles.$userId" to NetworkService.FieldOp.Delete
                )
            ),
            response = Boolean::class
        ) ?: throw IllegalStateException("Failed to leave shopping $shoppingId")
        Unit
    }

    override suspend fun resolveShortCode(shortCode: String): String? = withContext(ioDispatcher) {
        networkService.make<ShortCodeModel>(
            NetworkService.NetworkRequest(endpoint = "$SHORT_CODES/$shortCode", method = NetworkService.Method.GET)
        )?.shoppingId
            // ponytail: legacy query for lists shared before the index; denied once the AD-009 rules ship.
            ?: networkService.make<List<ShoppingModel>>(
                NetworkService.NetworkRequest(
                    endpoint = ENDPOINT,
                    method = NetworkService.Method.GET,
                    query = mapOf("shortCode" to shortCode)
                )
            )?.firstOrNull()?.id
    }

    override suspend fun ensureShortCodeIndex(shopping: Shopping): Unit = withContext(ioDispatcher) {
        val code = shopping.shortCode ?: return@withContext
        val existing = networkService.make<ShortCodeModel>(
            NetworkService.NetworkRequest(endpoint = "$SHORT_CODES/$code", method = NetworkService.Method.GET)
        )
        if (existing == null) writeShortCodeIndex(shopping)
    }

    private suspend fun writeShortCodeIndex(shopping: Shopping) {
        val code = shopping.shortCode ?: return
        networkService.make<String>(
            NetworkService.NetworkRequest(
                endpoint = SHORT_CODES,
                method = NetworkService.Method.POST,
                payload = ShortCodeModel(id = code, shoppingId = shopping.id).toMap()
            )
        )
    }

    override suspend fun updatePositions(shoppings: List<Shopping>): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            coroutineScope {
                shoppings.map { shopping ->
                    async {
                        networkService.make(
                            request = NetworkService.NetworkRequest(
                                endpoint = "$ENDPOINT/${shopping.id}",
                                method = NetworkService.Method.PUT,
                                payload = mapOf("position" to shopping.position)
                            ),
                            response = Boolean::class
                        )
                    }
                }.awaitAll()
            }
            Unit
        }
    }

    companion object {
        private const val ENDPOINT = "shopping"
        private const val SHORT_CODES = "shortCodes"
    }
}
