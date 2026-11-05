package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.exception.OwnershipRequiredException
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "delete_multi_shopping_list",
    description = "Deleta uma lista de compras com um ID específico"
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras",
    isRequired = true
)
@Singleton
internal class ShoppingMultiDeleteUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val authService: AuthService
) : AgentActionUseCase<Unit>() {

    override val requiresConfirmation: Boolean = true

    /** O título da lista que será apagada, para o card de aprovação. */
    override suspend fun confirmationSummary(arguments: Map<String, Any?>): String? =
        arguments.getString("shopping_id")?.let { repository.getById(it)?.title }

    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        val shopping = repository.getById(id) ?: throw Exception("Shopping not found")
        // Only the OWNER may delete the whole list — any collaborator could otherwise destroy
        // everyone else's data by just asking the chat to (sdd-017, B-10).
        val callerId = authService.currentUser?.id
        if (shopping.roles[callerId] != User.Role.OWNER.name) {
            throw OwnershipRequiredException("delete_multi_shopping_list")
        }
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
