package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Checks whether a product name matches an existing, not-yet-purchased item on a shopping list.
 *
 * NOTE: This is a minimal implementation created for PR5 (`feat/iaa-quick-add`) so Quick Add's
 * duplicate warning can ship independently. PR4 (`feat/iaa-duplicate-warning`, IAA-02) may add its
 * own copy of this use case in a parallel worktree — reconcile the two into a single shared use
 * case when the branches merge instead of keeping both.
 *
 * Matching rule (per `.specs/features/item-add-authorship/design.md`): trimmed, case-insensitive
 * name match against non-purchased products only. Returns the matching [Product] (so the caller
 * can read `.addedBy`) or `null` when there's no match.
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
