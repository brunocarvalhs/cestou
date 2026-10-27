package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import kotlinx.coroutines.flow.Flow

// One operation per use case; splitting the contract is out of scope here.
@Suppress("TooManyFunctions")
interface ShoppingRepository {
    /** Observes all shopping lists. Always emits a List (never null), may be empty if no lists exist. */
    fun observeAll(): Flow<List<Shopping>>
    /** Observes a specific shopping list. May emit null if the list doesn't exist or on access denied. */
    fun observeById(id: String): Flow<Shopping?>
    suspend fun getAll(): List<Shopping>
    suspend fun getById(id: String): Shopping?
    suspend fun create(shopping: Shopping)
    suspend fun update(shopping: Shopping)
    suspend fun delete(shopping: Shopping)
    suspend fun join(shoppingId: String, userId: String)
    /** Removes [userId] from the list's users and roles. Throws if the write fails. */
    suspend fun leave(shoppingId: String, userId: String)
    /** Resolves a share code to a list id without reading the list (non-members can't). */
    suspend fun resolveShortCode(shortCode: String): String?
    /** Writes the share-code index for [shopping] if it is missing (lists created before it existed). */
    suspend fun ensureShortCodeIndex(shopping: Shopping)
    suspend fun updatePositions(shoppings: List<Shopping>): Result<Unit>
    /**
     * Partial write: only the keys present in [fields] are changed in Firestore, everything
     * else keeps its stored value. Pass [br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService.FieldOp.Delete]
     * to clear a nullable field (a plain `null` value is dropped, not sent).
     */
    suspend fun updateFields(id: String, fields: Map<String, Any?>): Result<Unit>
}
