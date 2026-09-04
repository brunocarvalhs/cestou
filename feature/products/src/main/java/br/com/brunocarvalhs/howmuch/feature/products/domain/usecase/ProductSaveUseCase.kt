package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.ProductRepository
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@AiAgentAction(
    id = "save_product_to_cart",
    description = "Adiciona ou atualiza um produto no carrinho"
)
@AiAgentParameter(name = "name", description = "O nome do produto")
@AiAgentParameter(name = "quantity", description = "A quantidade do produto", isRequired = false)
@AiAgentParameter(name = "price", description = "O preço do produto", isRequired = false)
@AiAgentParameter(name = "shoppingId", description = "ID da lista de compras", isRequired = false)
@Singleton
class ProductSaveUseCase @Inject constructor(
    private val repository: ProductRepository,
    private val authService: AuthService,
    private val duplicateCheckUseCase: ProductDuplicateCheckUseCase,
    private val userRepository: UserRepository
) : AgentActionUseCase<String>() {

    suspend operator fun invoke(
        name: String,
        quantity: Double,
        shoppingId: String
    ): Result<Unit> {
        val product = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = quantity,
            price = 0.0
        )
        return invoke(product, shoppingId)
    }

    suspend operator fun invoke(
        product: Product,
        shoppingId: String
    ): Result<Unit> {
        val userId = authService.getOrCreateUserId().id
        return save(product, shoppingId, userId)
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<String> {
        val name = arguments.getString("name") ?: return Result.failure(Exception("Nome é obrigatório"))
        val shoppingId = arguments.getString("shoppingId")
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("ID da lista de compras não encontrado"))

        val product = Product(
            id = UUID.randomUUID().toString(),
            name = name,
            quantity = arguments["quantity"]?.toString()?.toDoubleOrNull() ?: 1.0,
            price = arguments["price"]?.toString()?.toDoubleOrNull() ?: 0.0
        )

        // The AI agent always acts on behalf of a human user, never as its own
        // identity (spec Edge Cases) — prefer the session's user id, falling back
        // to AuthService only if the session doesn't carry one.
        val userId = session.userId ?: authService.getOrCreateUserId().id

        // Duplicate check (spec item-add-authorship P2, IAA-02): informational only —
        // the item is saved either way (AC2, no hard block). Checked against the list
        // state *before* this save, so it only flags an item that was already there.
        val duplicate = duplicateCheckUseCase(name, shoppingId)

        val saveResult = save(product, shoppingId, userId)
        if (saveResult.isFailure) return saveResult.map { "" }

        // The AI chat is the only surface this PR wires the warning into: this
        // message is what feeds back into the model's function-call response
        // (see GeminiAiAgent/OpenRouterAiAgent: `.getOrNull()?.toString()`), so the
        // assistant can mention the duplicate naturally in its reply to the user.
        val message = if (duplicate != null) {
            val adderName = duplicate.addedBy?.let { userRepository.getUserProfile(it).first()?.name }
            "Produto adicionado. Atenção: \"${duplicate.name}\" já foi adicionado por " +
                "${adderName ?: "outra pessoa"} e ainda não foi comprado."
        } else {
            "Produto adicionado com sucesso."
        }
        return Result.success(message)
    }

    private suspend fun save(product: Product, shoppingId: String, userId: String): Result<Unit> =
        repository.saveProduct(product.withActivity(ProductActivity.Action.ADDED, userId), shoppingId)
}
