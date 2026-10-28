package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.ShoppingTexts
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

class ShoppingDuplicateUseCase @Inject constructor(
    private val texts: ShoppingTexts,
    private val repository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase,
    private val authService: AuthService
) {
    suspend operator fun invoke(shopping: Shopping): Result<Shopping> = runCatching {
        val newId = UUID.randomUUID().toString()
        val copySuffix = texts.copySuffix()
        val currentUserId = authService.getOrCreateUserId().id
        val duplicatedShopping = shopping.copy(
            id = newId,
            title = "${shopping.title} $copySuffix",
            status = Shopping.Status.NEW,
            createdAt = System.currentTimeMillis(),
            isFavorite = false,
            price = 0.0,
            // A duplicated list is a private copy, never a shared one: the duplicator becomes
            // the sole owner, and shortCode is dropped so it doesn't collide with the source
            // list's share-code index (see firestore.rules shortCodes `allow create`).
            users = listOf(currentUserId),
            roles = mapOf(currentUserId to User.Role.OWNER.name),
            shortCode = null
        )

        repository.create(duplicatedShopping)

        // first(): a single snapshot of the current products, not a subscription — getAllProducts
        // is backed by a Firestore listener Flow that never completes on its own.
        val products = productsUseCase(shopping.id).first()
        coroutineScope {
            products.map { product ->
                async {
                    // save(), not update(): the copy gets a brand-new id that was never written,
                    // and update() is a Firestore `.update()`, which fails with NOT_FOUND on it.
                    productsUseCase.save(
                        product.copy(id = UUID.randomUUID().toString(), isPurchased = false),
                        newId
                    ).getOrThrow()
                }
            }.awaitAll()
        }

        duplicatedShopping
    }
}
