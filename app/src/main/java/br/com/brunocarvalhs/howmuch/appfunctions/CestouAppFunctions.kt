package br.com.brunocarvalhs.howmuch.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.service.AppFunction
import androidx.appfunctions.service.AppFunctionEntryPoint
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.RecipeRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.RecipeAddToListUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.RecipeSearchUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingCreateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingGetAllUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Ponte entre as AgentActionUseCase já expostas ao agente de IA interno (core:ai)
 * e o AppFunctions do Android, para que o Gemini do sistema consiga chamar as
 * mesmas ações. Cobre um subconjunto representativo (não as 17 ações existentes)
 * evitando as duas que hoje colidem em id entre feature:shopping e feature:products
 * (update_shopping_list, clear_purchased_products).
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ShoppingListSummary(
    val id: String,
    val title: String,
    val description: String,
)

@AppFunctionSerializable(isDescribedByKDoc = true)
data class ProductSummary(
    val id: String,
    val name: String,
    val quantity: Double,
    val price: Double,
)

@AppFunctionSerializable(isDescribedByKDoc = true)
data class RecipeSummary(
    val id: String,
    val name: String,
    val description: String,
)

@RequiresApi(36)
@AndroidEntryPoint
@Suppress("RestrictedApi") // AppFunctionEntryPoint is @RestrictTo(LIBRARY_GROUP) in this alpha09
@AppFunctionEntryPoint(
    serviceName = "CestouAppFunctionService",
    appFunctionXmlFileName = "cestou_app_function_service",
)
abstract class BaseCestouAppFunctionService : AppFunctionService() {

    @Inject
    internal lateinit var shoppingCreateUseCase: ShoppingCreateUseCase

    @Inject
    internal lateinit var shoppingGetAllUseCase: ShoppingGetAllUseCase

    @Inject
    internal lateinit var productSearchUseCase: ProductSearchUseCase

    @Inject
    internal lateinit var productSaveUseCase: ProductSaveUseCase

    @Inject
    internal lateinit var recipeSearchUseCase: RecipeSearchUseCase

    @Inject
    internal lateinit var recipeAddToListUseCase: RecipeAddToListUseCase

    @Inject
    internal lateinit var recipeRepository: RecipeRepository

    /**
     * Cria uma nova lista de compras.
     *
     * @param title Título da lista de compras.
     * @param description Descrição opcional da lista.
     * @return A lista de compras recém-criada.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createShoppingList(
        title: String,
        description: String?,
    ): ShoppingListSummary {
        val shopping = shoppingCreateUseCase(title = title, description = description)
            .getOrElse { throw IllegalStateException("Falha ao criar a lista: ${it.message}") }
        return ShoppingListSummary(
            id = shopping.id,
            title = shopping.title,
            description = shopping.description
        )
    }

    /**
     * Retorna todas as listas de compras do usuário.
     *
     * @return A lista de listas de compras existentes.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getAllShoppingLists(): List<ShoppingListSummary> {
        val shoppingLists = shoppingGetAllUseCase()
            .getOrElse { throw IllegalStateException("Falha ao buscar as listas: ${it.message}") }
        return shoppingLists.map {
            ShoppingListSummary(id = it.id, title = it.title, description = it.description)
        }
    }

    /**
     * Busca produtos disponíveis pelo nome.
     *
     * @param query Termo de busca, como o nome do produto.
     * @return Produtos encontrados.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchProducts(query: String): List<ProductSummary> {
        val products = productSearchUseCase(query)
            .getOrElse { throw IllegalStateException("Falha ao buscar produtos: ${it.message}") }
        return products.map {
            ProductSummary(id = it.id, name = it.name, quantity = it.quantity, price = it.price)
        }
    }

    /**
     * Adiciona um produto a uma lista de compras existente.
     *
     * @param name Nome do produto.
     * @param quantity Quantidade do produto.
     * @param shoppingId ID da lista de compras de destino.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun saveProductToCart(
        name: String,
        quantity: Double,
        shoppingId: String,
    ) {
        productSaveUseCase(name = name, quantity = quantity, shoppingId = shoppingId)
            .getOrElse { throw IllegalStateException("Falha ao adicionar o produto: ${it.message}") }
    }

    /**
     * Busca receitas culinárias pelo nome ou ingredientes.
     *
     * @param query Termo de busca, como o nome da receita.
     * @return Receitas encontradas.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun searchRecipes(query: String): List<RecipeSummary> {
        val recipes = recipeSearchUseCase(query)
            .getOrElse { throw IllegalStateException("Falha ao buscar receitas: ${it.message}") }
        return recipes.map {
            RecipeSummary(id = it.id, name = it.name, description = it.description)
        }
    }

    /**
     * Adiciona todos os ingredientes de uma receita a uma lista de compras.
     *
     * @param recipeId ID da receita, obtido via searchRecipes.
     * @param shoppingId ID da lista de compras de destino.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun addRecipeIngredientsToList(
        recipeId: String,
        shoppingId: String,
    ) {
        val recipe = recipeRepository.getRecipeById(recipeId)
            .getOrElse { throw IllegalStateException("Falha ao buscar a receita: ${it.message}") }
            ?: throw AppFunctionElementNotFoundException("Receita não encontrada para o id informado")

        recipeAddToListUseCase(recipe, shoppingId)
            .getOrElse { throw IllegalStateException("Falha ao adicionar os ingredientes: ${it.message}") }
    }
}
