package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.CommonProductRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "add_common_products_to_cart",
    description = "Adiciona todos os itens da cesta básica (produtos comuns) do usuário na lista de compras atual"
)
@AiAgentParameter(name = "shoppingId", description = "ID da lista de compras", isRequired = false)
@Singleton
class CommonProductAddAllToShoppingUseCase @Inject constructor(
    private val commonProductRepository: CommonProductRepository,
    private val productRepository: ProductRepository
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(shoppingId: String): Result<Unit> = runCatching {
        commonProductRepository.seedDefaultsIfEmpty()
        val items = commonProductRepository.getAll().first()
        items.forEach { item ->
            productRepository.saveProduct(
                Product(
                    id = UUID.randomUUID().toString(),
                    name = item.name,
                    quantity = 1.0,
                    price = 0.0,
                    category = item.category,
                    unit = item.unit
                ),
                shoppingId
            )
        }
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val shoppingId = arguments.getString("shoppingId")
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("ID da lista de compras não encontrado"))

        return invoke(shoppingId)
    }
}
