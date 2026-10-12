package br.com.brunocarvalhs.howmuch.core.domain.usecase

/**
 * AD-011: contract consumed by `feature/cart`. Implemented by `ShoppingClearPurchasedUseCaseImpl`
 * in `feature/products` (which also exposes it as an AI agent action).
 */
interface ShoppingClearPurchasedUseCase {
    suspend operator fun invoke(shoppingId: String): Result<Unit>
}
