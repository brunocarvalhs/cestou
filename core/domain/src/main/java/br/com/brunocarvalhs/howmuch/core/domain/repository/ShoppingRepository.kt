package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import kotlinx.coroutines.flow.Flow

interface ShoppingRepository {
    fun observeAll(): Flow<List<Shopping>>
    fun observeById(id: String): Flow<Shopping?>
    suspend fun getAll(): List<Shopping>
    suspend fun getById(id: String): Shopping?
    suspend fun create(shopping: Shopping)
    suspend fun update(shopping: Shopping)
    suspend fun delete(shopping: Shopping)
    suspend fun join(shoppingId: String, userId: String)
    suspend fun getByShortCode(shortCode: String): Shopping?
    suspend fun updatePositions(shoppings: List<Shopping>): Result<Unit>
}
