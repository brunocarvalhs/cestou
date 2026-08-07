package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "save_product_to_cart",
    description = "Adiciona ou atualiza um produto no carrinho"
)
@AiAgentParameter(name = "name", description = "O nome do produto")
@AiAgentParameter(name = "quantity", description = "A quantidade do produto", isRequired = false)
@AiAgentParameter(name = "price", description = "O preço do produto", isRequired = false)
@AiAgentParameter(name = "shoppingId", description = "ID da lista de compras", isRequired = false)
@Singleton
class ProductSaveUseCase @Inject constructor(
    private val repository: ProductRepository
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(
        name: String,
        quantity: Double,
        shoppingId: String
    ): Result<Unit> {
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = quantity,
            price = 0.0
        )
        return invoke(product, shoppingId)
    }

    suspend operator fun invoke(
        product: Product,
        shoppingId: String
    ): Result<Unit> = repository.saveProduct(product, shoppingId)

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val name = arguments.getString("name") ?: return Result.failure(Exception("Nome é obrigatório"))
        val shoppingId = arguments.getString("shoppingId") 
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("ID da lista de compras não encontrado"))

        val product = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = arguments["quantity"]?.toString()?.toDoubleOrNull() ?: 1.0,
            price = arguments["price"]?.toString()?.toDoubleOrNull() ?: 0.0
        )

        return invoke(product, shoppingId)
    }
}
