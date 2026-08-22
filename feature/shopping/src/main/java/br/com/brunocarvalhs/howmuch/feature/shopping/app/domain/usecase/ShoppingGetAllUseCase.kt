package br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import javax.inject.Inject

@AiAgentAction(
    id = "get_all_shopping_lists",
    description = "Retorna todas as listas de compras"
)
class ShoppingGetAllUseCase @Inject constructor(
    private val repository: ShoppingRepository
): AgentActionUseCase<List<Shopping>>() {
    suspend operator fun invoke(): Result<List<Shopping>> = runCatching {
        repository.getAll()
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<List<Shopping>> {
        return invoke()
    }
}
