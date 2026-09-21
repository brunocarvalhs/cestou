package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "delete_shopping_list",
    description = "Deleta uma lista de compras com um ID específico"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
class ShoppingDeleteUseCase @Inject constructor(
    private val repository: ShoppingRepository
): AgentActionUseCase<Unit>() {

    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        val shopping = repository.getById(id) ?: throw Exception("Shopping not found")
        repository.delete(shopping)
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        return invoke(id)
    }
}
