package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.repository.RecipeRepository
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.RecipeAddToListUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeAddToListUseCaseTest {

    private val saveUseCase = mockk<ProductSaveUseCase>(relaxed = true)
    private val recipeRepository = mockk<RecipeRepository>()
    private val useCase = RecipeAddToListUseCase(saveUseCase, recipeRepository)

    private val recipe = Recipe(
        id = "r1",
        name = "Bolo",
        description = "desc",
        ingredients = listOf(Product(id = "p1", name = "Flour", quantity = 1.0, price = 2.0))
    )

    @Test
    fun `execute adds every ingredient of the found recipe to the shopping list`() = runTest {
        coEvery { recipeRepository.getRecipeById("r1") } returns Result.success(recipe)

        val result = useCase.execute(
            mapOf("recipe_id" to "r1", "shopping_id" to "list1"),
            mockk(relaxed = true),
            emptyMap()
        )

        assertTrue(result.isSuccess)
        coVerify { saveUseCase(match<Product> { it.name == "Flour" }, "list1") }
    }

    @Test
    fun `execute fails when the recipe is not found`() = runTest {
        coEvery { recipeRepository.getRecipeById("missing") } returns Result.success(null)

        val result = useCase.execute(
            mapOf("recipe_id" to "missing", "shopping_id" to "list1"),
            mockk(relaxed = true),
            emptyMap()
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when the repository call fails`() = runTest {
        coEvery { recipeRepository.getRecipeById("r1") } returns Result.failure(IllegalStateException("boom"))

        val result = useCase.execute(
            mapOf("recipe_id" to "r1", "shopping_id" to "list1"),
            mockk(relaxed = true),
            emptyMap()
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when recipe_id is missing`() = runTest {
        val result = useCase.execute(mapOf("shopping_id" to "list1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }

    @Test
    fun `execute fails when shopping_id is missing from both arguments and metadata`() = runTest {
        val result = useCase.execute(mapOf("recipe_id" to "r1"), mockk(relaxed = true), emptyMap())

        assertTrue(result.isFailure)
    }
}
