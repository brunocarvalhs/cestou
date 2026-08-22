package br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductsUseCase
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AiAgentAction(
    id = "get_shopping_list_details",
    description = "Obtém detalhes de uma lista de compras específica, incluindo todos os produtos atuais e o orçamento."
)
@AiAgentParameter(
    name = "shoppingId",
    description = "O ID da lista de compras (opcional se estiver no contexto)",
    isRequired = false
)
class ShoppingGetDetailsUseCase @Inject constructor(
    private val shoppingRepository: ShoppingRepository,
    private val productsUseCase: ProductsUseCase
) : AgentActionUseCase<Map<String, Any?>>() {

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Map<String, Any?>> {
        val shoppingId = arguments.getString("shoppingId")
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("ID da lista de compras não encontrado"))

        return runCatching {
            val shopping = shoppingRepository.getById(shoppingId)
                ?: throw Exception("Lista de compras não encontrada")
            
            val products = productsUseCase(shoppingId).first()

            mapOf(
                "title" to shopping.title,
                "description" to shopping.description,
                "budget" to shopping.budget,
                "totalSpent" to products.sumOf { it.total },
                "products" to products.map { 
                    mapOf(
                        "name" to it.name,
                        "quantity" to it.quantity,
                        "unit" to it.unit,
                        "price" to it.price,
                        "isPurchased" to it.isPurchased,
                        "category" to it.category
                    )
                }
            )
        }
    }
}
