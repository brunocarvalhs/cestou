package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.ProductRepository
import javax.inject.Inject

@AiAgentAction(
    id = "search_products",
    description = "Busca produtos disponíveis ou sugestões por nome"
)
@AiAgentParameter(
    name = "query",
    description = "O termo de busca (ex: nome do produto)",
    isRequired = true
)
class ProductSearchUseCase @Inject constructor(
    private val repository: ProductRepository
) : AgentActionUseCase<List<Product>>() {

    suspend operator fun invoke(query: String): Result<List<Product>> = repository.searchProducts(query)

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<List<Product>> {
        val query = arguments.getString("query") ?: ""
        return invoke(query)
    }
}
