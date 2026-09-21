package br.com.brunocarvalhs.howmuch.feature.products.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.products.domain.model.Recipe
import br.com.brunocarvalhs.howmuch.feature.products.domain.repository.RecipeRepository
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.RecipeSearchUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSearchUseCaseTest {

    private val repository = mockk<RecipeRepository>()
    private val useCase = RecipeSearchUseCase(repository)

    @Test
    fun `invoke delegates a non-blank query to the repository`() = runTest {
        val recipe = Recipe(id = "r1", name = "Bolo", description = "desc", ingredients = emptyList())
        coEvery { repository.searchRecipes("bolo") } returns Result.success(listOf(recipe))

        val result = useCase("bolo")

        assertEquals(listOf(recipe), result.getOrNull())
    }

    @Test
    fun `invoke returns an empty list without calling the repository for a blank query`() = runTest {
        val result = useCase("   ")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Recipe>(), result.getOrNull())
        io.mockk.coVerify(exactly = 0) { repository.searchRecipes(any()) }
    }
}
