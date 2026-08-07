package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.model.AiAgentSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.feature.products.domain.entity.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
import java.util.UUID
import javax.inject.Inject

@AiAgentAction(
    id = "add_recipe_ingredients",
    description = "Adiciona todos os ingredientes de uma receita específica à lista de compras atual"
)
@AiAgentParameter(
    name = "recipe_id",
    description = "O ID da receita cujos ingredientes devem ser adicionados",
    isRequired = true
)
@AiAgentParameter(
    name = "shopping_id",
    description = "O ID da lista de compras de destino (opcional se estiver no contexto)",
    isRequired = false
)
class RecipeAddToListUseCase @Inject constructor(
    private val saveUseCase: ProductSaveUseCase,
    private val recipeRepository: RecipeRepository
) : AgentActionUseCase<Unit>() {

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiAgentSession,
        metadata: Map<String, Any?>
    ): Result<Unit> {
        val recipeId = arguments.getString("recipe_id") ?: return Result.failure(Exception("recipe_id missing"))
        val shoppingId = arguments.getString("shopping_id")
            ?: metadata.getString("shopping")
            ?: return Result.failure(Exception("shopping_id missing"))

        return recipeRepository.getRecipeById(recipeId).fold(
            onSuccess = { recipe ->
                if (recipe != null) {
                    invoke(recipe, shoppingId)
                } else {
                    Result.failure(Exception("Recipe not found"))
                }
            },
            onFailure = { Result.failure(it) }
        )
    }

    suspend operator fun invoke(recipe: Recipe, shoppingId: String): Result<Unit> {
        return runCatching {
            recipe.ingredients.forEach { product ->
                saveUseCase(
                    product = product.copy(id = UUID.randomUUID().toString()),
                    shoppingId = shoppingId
                )
            }
        }
    }
}
