package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "reopen_shopping_list",
    description = "Reabre uma lista de compras finalizada, movendo-a para o status EM PROGRESSO"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
internal class ShoppingReopenUseCase @Inject constructor(
    private val repository: ShoppingRepository
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(shopping: Shopping): Result<Unit> = runCatching {
        repository.update(shopping.copy(status = Shopping.Status.IN_PROGRESS))
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val shoppingId = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        val shopping = repository.getById(shoppingId) ?: throw Exception("Shopping not found")
        return invoke(shopping)
    }
}
