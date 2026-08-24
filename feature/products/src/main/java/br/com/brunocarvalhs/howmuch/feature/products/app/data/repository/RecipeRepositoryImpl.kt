package br.com.brunocarvalhs.howmuch.feature.products.app.data.repository

import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.domain.services.make
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.RecipeRepository
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

internal class RecipeRepositoryImpl @Inject constructor(
    @Named("CloudNetwork") private val networkService: NetworkService
) : RecipeRepository {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY
        )
    }

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
            val recipes = meals?.map { it.jsonObject.toRecipe() } ?: emptyList()
            translateAll(recipes)
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
            meals?.firstOrNull()?.jsonObject?.toRecipe()?.let { translateToPortuguese(it) }
        }
    }

    private suspend fun translateAll(recipes: List<Recipe>): List<Recipe> = coroutineScope {
        recipes.map { recipe -> async { translateToPortuguese(recipe) } }.awaitAll()
    }

    /**
     * TheMealDB only publishes content in English. Translating here (instead of swapping
     * data source) keeps its large, stable dataset while giving users PT-BR text.
     */
    private suspend fun translateToPortuguese(recipe: Recipe): Recipe = runCatching {
        val response = generativeModel.generateContent(Helper.createTranslationPrompt(recipe))
        val fullText = response.text ?: return@runCatching recipe

        val startIndex = fullText.indexOf("{")
        val endIndex = fullText.lastIndexOf("}")
        if (startIndex == -1 || endIndex == -1 || endIndex <= startIndex) return@runCatching recipe

        val json = Json.parseToJsonElement(fullText.substring(startIndex, endIndex + 1)).jsonObject
        val translatedIngredients = json["ingredients"]?.jsonArray

        recipe.copy(
            name = json["name"]?.jsonPrimitive?.content ?: recipe.name,
            description = json["description"]?.jsonPrimitive?.content ?: recipe.description,
            instructions = json["instructions"]?.jsonPrimitive?.content ?: recipe.instructions,
            ingredients = recipe.ingredients.mapIndexed { index, ingredient ->
                val translatedName = translatedIngredients?.getOrNull(index)?.jsonPrimitive?.content
                if (translatedName.isNullOrBlank()) ingredient else ingredient.copy(name = translatedName)
            }
        )
    }.getOrElse {
        Timber.e(it, "Falha ao traduzir receita '${recipe.name}', mantendo texto original")
        recipe
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

    private object Helper {
        fun createTranslationPrompt(recipe: Recipe): String {
            val ingredientsJson = recipe.ingredients.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { Json.encodeToString(String.serializer(), it.name) }

            return """
                Traduza o conteúdo de receita culinária abaixo do inglês para português do Brasil.
                Mantenha quantidades e unidades de medida (traduza também os nomes das unidades, ex: "cup" -> "xícara").
                Não invente informação nova, apenas traduza. Não inclua nenhuma explicação fora do JSON.

                Nome: ${recipe.name}
                Descrição: ${recipe.description}
                Instruções: ${recipe.instructions ?: ""}
                Ingredientes (array JSON, mantenha exatamente a mesma quantidade e ordem de itens): $ingredientsJson

                Responda apenas com um objeto JSON válido no formato exato:
                {"name": "...", "description": "...", "instructions": "...", "ingredients": ["...", "..."]}
            """.trimIndent()
        }
    }

    private companion object {
        const val MAX_INGREDIENTS = 20
    }
}
