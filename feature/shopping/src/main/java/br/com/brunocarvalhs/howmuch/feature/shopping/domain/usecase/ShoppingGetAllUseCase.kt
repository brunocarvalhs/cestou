package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
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
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<List<Shopping>> {
        return invoke()
    }
}
