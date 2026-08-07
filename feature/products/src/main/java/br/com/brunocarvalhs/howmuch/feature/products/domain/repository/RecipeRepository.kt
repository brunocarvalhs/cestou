package br.com.brunocarvalhs.howmuch.feature.products.domain.repository

import br.com.brunocarvalhs.howmuch.feature.products.domain.entity.Recipe

interface RecipeRepository {
    suspend fun searchRecipes(query: String): Result<List<Recipe>>
    suspend fun getRecipeById(id: String): Result<Recipe?>
}
