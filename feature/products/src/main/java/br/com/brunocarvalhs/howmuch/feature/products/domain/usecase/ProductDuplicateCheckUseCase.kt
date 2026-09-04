package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Checks whether [shoppingId] already has an active (not yet purchased) product whose name
 * matches [name], ignoring case and leading/trailing whitespace.
 *
 * Used to power the "already on the list" warning (spec `item-add-authorship` P2, IAA-02): the
 * warning is informational only — callers SHALL still save the item regardless of the result
 * (no hard block, spec P2 AC2). Returns the matching [Product] (so the caller can read
 * `.addedBy`) or `null` when there's no active match.
 *
 * Shared by both the AI add path (`ProductSaveUseCase.execute`) and the Quick Add screen
 * (`QuickAddViewModel`) — reconciled here after PR4 and PR5 each added their own copy in
 * isolated worktrees.
 */
class ProductDuplicateCheckUseCase @Inject constructor(
    private val repository: ProductRepository
) {
    suspend operator fun invoke(name: String, shoppingId: String): Product? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null

        return repository.getAllProducts(shoppingId).first()
            .firstOrNull { !it.isPurchased && it.name.trim().equals(trimmedName, ignoreCase = true) }
    }
}
