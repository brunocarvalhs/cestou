package br.com.brunocarvalhs.howmuch.core.domain.repository

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * AD-011: the minimal, read-only capability external consumers (e.g. `feature/shopping`,
 * `feature/cart`) actually need from Products. `feature/products`' `ProductRepository` is a
 * superset of this contract (adds write operations) — see `ProductRepository : ProductReader`.
 */
interface ProductReader {
    suspend fun getAllProducts(shoppingId: String): Flow<List<Product>>
}
