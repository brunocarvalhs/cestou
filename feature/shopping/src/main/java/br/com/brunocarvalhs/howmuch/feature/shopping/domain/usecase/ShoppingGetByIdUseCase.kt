package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AgentAction
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository

import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "open_shopping_by_id",
    description = "Abre uma lista de compras com um ID específico"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
internal class ShoppingGetByIdUseCase @Inject constructor(
    private val repository: ShoppingRepository
): AgentActionUseCase<Shopping>() {
    suspend operator fun invoke(id: String): Result<Shopping> = runCatching {
        repository.getById(id) ?: throw Exception("Shopping not found")
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Shopping> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        return invoke(id)
    }
}