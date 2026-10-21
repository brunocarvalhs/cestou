package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
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
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        // This is a partial update: the AI agent may be asked to change a single
        // field (e.g. "change the price to 50"), so it should not be forced to
        // resend every field of the list on every call. Only the fields present
        // in `arguments` are overridden; everything else keeps the value already
        // stored for this list (see Crashlytics non-fatal "Shopping title is
        // required" — the LLM wasn't wrong, the tool contract was).
        val current = repository.getById(id) ?: throw Exception("Shopping list not found")
        val shopping = current.copy(
            title = arguments.getString("title") ?: current.title,
            description = arguments.getString("description") ?: current.description,
            price = arguments.getDouble("price") ?: current.price,
            status = arguments.getString("status")
                ?.let { Shopping.Status.valueOf(it) }
                ?: current.status,
            users = if (arguments.containsKey("users")) arguments.getList("users") else current.users,
            roles = if (arguments.containsKey("roles")) arguments.getMap("roles") else current.roles,
        )
        return invoke(id, shopping)
    }
}
