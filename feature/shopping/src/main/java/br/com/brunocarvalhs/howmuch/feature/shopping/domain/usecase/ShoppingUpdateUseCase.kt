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
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.exception.OwnershipRequiredException
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
internal class ShoppingUpdateUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val authService: AuthService
) : AgentActionUseCase<Unit>() {

    suspend operator fun invoke(
        id: String,
        fields: Map<String, Any?>
    ): Result<Unit> = repository.updateFields(id, fields)

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val id = arguments.getString("shopping_id") ?: throw Exception("Shopping ID is required")
        val shopping = repository.getById(id) ?: throw Exception("Shopping list not found")
        // This is a partial update: the AI agent may be asked to change a single
        // field (e.g. "change the price to 50"), so it should not be forced to
        // resend every field of the list on every call, and the write itself only
        // touches the keys below in Firestore — it can no longer clobber a field
        // another member changed concurrently (see Crashlytics non-fatal "Shopping
        // title is required" — the LLM wasn't wrong, the tool contract was).
        val fields = buildMap<String, Any?> {
            arguments.getString("title")?.let { put("title", it) }
            arguments.getString("description")?.let { put("description", it) }
            arguments.getDouble("price")?.let { put("price", it) }
            arguments.getString("status")
                // Tolerant to casing: the LLM may send "finished" instead of "FINISH" (sdd-017, C-03).
                ?.let { status -> Shopping.Status.entries.firstOrNull { it.name.equals(status, ignoreCase = true) } }
                ?.let { put("status", it.name) }
            if (arguments.containsKey("users") || arguments.containsKey("roles")) {
                // Only the OWNER may reassign ownership/roles — any other member could otherwise
                // strip the real owner's access by just asking the chat to (sdd-017, B-02).
                val callerId = authService.currentUser?.id
                if (shopping.roles[callerId] != User.Role.OWNER.name) {
                    return Result.failure(OwnershipRequiredException("update_shopping_list.users_roles"))
                }
                if (arguments.containsKey("users")) put("users", arguments.getList("users"))
                if (arguments.containsKey("roles")) put("roles", arguments.getMap("roles"))
            }
        }
        return invoke(id, fields)
    }
}
