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
    id = "add_multiple_products_to_cart",
    description = "Adiciona uma lista de produtos ao carrinho de uma só vez"
)
@AiAgentParameter(name = "productNames", description = "Lista de nomes dos produtos separados por vírgula")
@AiAgentParameter(name = "shoppingId", description = "ID da lista de compras", isRequired = false)
@Singleton
internal class ProductListSaveUseCase @Inject constructor(
    private val repository: ProductRepository
) : AgentActionUseCase<List<Product>>() {

    suspend operator fun invoke(
        products: List<Product>,
        shoppingId: String
    ): Result<List<Product>> {
        val savedProducts = mutableListOf<Product>()
        products.forEach { product ->
            repository.saveProduct(product, shoppingId)
                .onFailure { return Result.failure(it) }
            savedProducts += product
        }
        return Result.success(savedProducts)
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<List<Product>> {
        val names = arguments.getString("productNames")?.split(",")?.map { it.trim() }
            ?: return Result.failure(Exception("Lista de nomes é obrigatória"))

        val shoppingId = arguments.getString("shoppingId")
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("ID da lista de compras não encontrado"))

        val products = names.map { name ->
            Product(
                id = UUID.randomUUID().toString(),
                name = name,
                quantity = 1.0,
                price = 0.0
            )
        }

        return invoke(products, shoppingId)
    }
}
