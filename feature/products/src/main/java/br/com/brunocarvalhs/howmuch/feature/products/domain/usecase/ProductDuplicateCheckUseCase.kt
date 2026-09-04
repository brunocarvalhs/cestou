package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Checks whether [shoppingId] already has an active (not yet purchased) product whose name
 * matches [name], ignoring case and leading/trailing whitespace.
 *
 * Used to power the "already on the list" warning (spec `item-add-authorship` P2, IAA-02): the
 * warning is informational only — callers SHALL still save the item regardless of the result
 * (no hard block, spec P2 AC2).
 */
class ProductDuplicateCheckUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(name: String, shoppingId: String): Product? =
        repository.getAllProducts(shoppingId).first()
            .firstOrNull { !it.isPurchased && it.name.trim().equals(name.trim(), ignoreCase = true) }
}
