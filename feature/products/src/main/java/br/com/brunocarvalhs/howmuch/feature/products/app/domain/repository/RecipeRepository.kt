package br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository

import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.Recipe

interface RecipeRepository {
    suspend fun searchRecipes(query: String): Result<List<Recipe>>
    suspend fun getRecipeById(id: String): Result<Recipe?>
}
