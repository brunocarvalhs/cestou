package br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.CommonProduct
import kotlinx.coroutines.flow.Flow

interface CommonProductRepository {
    fun getAll(): Flow<List<CommonProduct>>
    suspend fun seedDefaultsIfEmpty(): Result<Unit>
    suspend fun add(item: CommonProduct): Result<Unit>
    suspend fun remove(id: String): Result<Unit>
}
