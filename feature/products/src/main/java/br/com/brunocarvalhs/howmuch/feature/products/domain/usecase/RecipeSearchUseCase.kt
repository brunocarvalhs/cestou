package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentAction
import br.com.brunocarvalhs.howmuch.core.ai.annotation.AiAgentParameter
import br.com.brunocarvalhs.howmuch.core.ai.base.AgentActionUseCase
import br.com.brunocarvalhs.howmuch.core.ai.contract.AiSession
import br.com.brunocarvalhs.howmuch.core.ai.utils.getString
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
import javax.inject.Inject

@AiAgentAction(
    id = "search_recipes",
    description = "Busca receitas culinárias pelo nome ou ingredientes"
)
@AiAgentParameter(
    name = "query",
    description = "O termo de busca (ex: nome da receita)",
    isRequired = true
)
class RecipeSearchUseCase @Inject constructor(
    private val repository: RecipeRepository
) : AgentActionUseCase<List<Recipe>>() {

    suspend operator fun invoke(query: String): Result<List<Recipe>> {
        if (query.isBlank()) return Result.success(emptyList())
        return repository.searchRecipes(query)
    }

    override suspend fun execute(
        arguments: Map<String, Any?>,
        session: AiSession,
        metadata: Map<String, Any?>
    ): Result<List<Recipe>> {
        val query = arguments.getString("query") ?: ""
        return invoke(query)
    }
}
