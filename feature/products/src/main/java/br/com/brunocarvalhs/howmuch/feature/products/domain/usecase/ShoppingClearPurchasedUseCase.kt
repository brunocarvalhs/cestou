package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "clear_purchased_products",
    description = "Remove todos os itens marcados como comprados de uma lista de compras"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
class ShoppingClearPurchasedUseCase @Inject constructor(
    private val productRepository: ProductRepository
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(shoppingId: String): Result<Unit> = runCatching {
        val products = productRepository.getAllProducts(shoppingId).first()
        products.filter { it.isPurchased }.forEach { product ->
            productRepository.deleteProduct(product.id, shoppingId)
        }
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val shoppingId = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        return invoke(shoppingId)
    }
}
