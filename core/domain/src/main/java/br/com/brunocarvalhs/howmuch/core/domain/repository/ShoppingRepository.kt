package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import kotlinx.coroutines.flow.Flow

// One operation per use case; splitting the contract is out of scope here.
@Suppress("TooManyFunctions")
interface ShoppingRepository {
    fun observeAll(): Flow<List<Shopping>>
    fun observeById(id: String): Flow<Shopping?>
    suspend fun getAll(): List<Shopping>
    suspend fun getById(id: String): Shopping?
    suspend fun create(shopping: Shopping)
    suspend fun update(shopping: Shopping)
    suspend fun delete(shopping: Shopping)
    suspend fun join(shoppingId: String, userId: String)
    /** Removes [userId] from the list's users and roles. Throws if the write fails. */
    suspend fun leave(shoppingId: String, userId: String)
    suspend fun getByShortCode(shortCode: String): Shopping?
    suspend fun updatePositions(shoppings: List<Shopping>): Result<Unit>
}
