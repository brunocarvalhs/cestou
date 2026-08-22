package br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getDouble
import br.com.brunocarvalhs.howmuch.core.ai.utils.getList
import br.com.brunocarvalhs.howmuch.core.ai.utils.getMap
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "update_shopping_list",
    description = "Atualiza uma lista de compras com um ID específico"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
class ShoppingUpdateUseCase @Inject constructor(
    private val repository: ShoppingRepository
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(
        id: String,
        updatedShopping: Shopping
    ): Result<Unit> = runCatching {
        repository.update(updatedShopping)
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        val shopping = Shopping(
            id = id,
            title = arguments.getString("title") ?: throw Exception("Shopping title is required"),
            description = arguments.getString("description") ?: throw Exception("Shopping description is required"),
            price = arguments.getDouble("price") ?: throw Exception("Shopping price is required"),
            status = Shopping.Status.valueOf(
                arguments.getString("status") ?: throw Exception("Shopping status is required")
            ),
            users = arguments.getList("users"),
            roles = arguments.getMap("roles"),
        )
        return invoke(id, shopping)
    }
}
