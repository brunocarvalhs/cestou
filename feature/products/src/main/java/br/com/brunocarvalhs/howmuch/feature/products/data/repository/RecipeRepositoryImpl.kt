package br.com.brunocarvalhs.howmuch.feature.products.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.entity.Product
import br.com.brunocarvalhs.howmuch.core.domain.service.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.service.make
import br.com.brunocarvalhs.howmuch.feature.products.domain.entity.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

internal class RecipeRepositoryImpl @Inject constructor(
    @Named("CloudNetwork") private val networkService: NetworkService
) : RecipeRepository {

    override suspend fun searchRecipes(query: String): Result<List<Recipe>> {
        return runCatching {
            val response = networkService.make<JsonObject>(
                request = NetworkService.NetworkRequest(
                    endpoint = "https://www.themealdb.com/api/json/v1/1/search.php",
                    query = mapOf("s" to query),
                    method = NetworkService.Method.GET
                )
            )

            val meals = response?.get("meals")?.jsonArray
            meals?.map { it.jsonObject.toRecipe() } ?: emptyList()
        }
    }

    override suspend fun getRecipeById(id: String): Result<Recipe?> {
        return runCatching {
            val response = networkService.make<JsonObject>(
                request = NetworkService.NetworkRequest(
                    endpoint = "https://www.themealdb.com/api/json/v1/1/lookup.php",
                    query = mapOf("i" to id),
                    method = NetworkService.Method.GET
                )
            )

            val meals = response?.get("meals")?.jsonArray
            meals?.firstOrNull()?.jsonObject?.toRecipe()
        }
    }

    private fun JsonObject.toRecipe(): Recipe {
        val ingredients = mutableListOf<Product>()

        for (i in 1..MAX_INGREDIENTS) {
            val name = this["strIngredient$i"]?.jsonPrimitive?.content
            val measure = this["strMeasure$i"]?.jsonPrimitive?.content

            if (!name.isNullOrBlank()) {
                ingredients.add(
                    Product(
                        id = UUID.randomUUID().toString(),
                        name = "${measure ?: ""} $name".trim(),
                        quantity = 1.0,
                        price = 0.0
                    )
                )
            }
        }

        return Recipe(
            id = this["idMeal"]?.jsonPrimitive?.content ?: UUID.randomUUID().toString(),
            name = this["strMeal"]?.jsonPrimitive?.content ?: "",
            description = "${this["strCategory"]?.jsonPrimitive?.content ?: ""} - " +
                (this["strArea"]?.jsonPrimitive?.content ?: ""),
            instructions = this["strInstructions"]?.jsonPrimitive?.content ?: "",
            ingredients = ingredients,
            imageUrl = this["strMealThumb"]?.jsonPrimitive?.content
        )
    }

    private companion object {
        const val MAX_INGREDIENTS = 20
    }
}
