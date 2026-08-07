package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.entity.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.entity.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.service.AuthService
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "create_shopping_list",
    description = "Cria uma nova lista de compras com um título específico"
)
@AiAgentParameter(
    name = "title",
    description = "O nome ou título da lista de compras",
    isRequired = true
)
@AiAgentParameter(
    name = "description",
    description = "O nome ou título da lista de compras",
    isRequired = false
)
@Singleton
class ShoppingCreateUseCase @Inject constructor(
    private val repository: ShoppingRepository,
    private val authService: AuthService,
) : AgentActionUseCase<Shopping>() {

    suspend operator fun invoke(
        title: String = "Lista de compras",
        description: String = "Lista de compras padrão"
    ): Result<Shopping> = runCatching {
        val user = authService.getOrCreateUserId()
        val userId = user.id

        val shopping = Shopping(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            price = 0.0,
            status = Shopping.Status.NEW,
            users = listOf(userId),
            roles = mapOf(userId to User.Role.OWNER.name),
            shortCode = (1..6).map { (('A'..'Z') + ('0'..'9')).random() }.joinToString("")
        )

        repository.create(shopping)

        shopping
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Shopping> {
        val title = arguments.getString("title") ?: "Nova Lista"
        val description = arguments.getString("description") ?: "Lista de compras padrão"
        return invoke(title, description)
    }
}
