package br.com.brunocarvalhs.howmuch.feature.products.data.repository

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val repository = RecipeRepositoryImpl(networkService)

    @Test
    fun `searchRecipes returns an empty list without translating anything when no meals match`() = runTest {
        val json = Json.parseToJsonElement("""{}""") as JsonObject
        coEvery { networkService.make<JsonObject>(any(), any(), any()) } returns json

        val result = repository.searchRecipes("xyz-does-not-exist")

        assertTrue(result.isSuccess)
        assertEquals(emptyList<Any>(), result.getOrThrow())
    }

    @Test
    fun `searchRecipes fails when the network call throws`() = runTest {
        coEvery { networkService.make<JsonObject>(any(), any(), any()) } throws IllegalStateException("offline")

        val result = repository.searchRecipes("arroz")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getRecipeById returns null when no meal matches the id`() = runTest {
        val json = Json.parseToJsonElement("""{}""") as JsonObject
        coEvery { networkService.make<JsonObject>(any(), any(), any()) } returns json

        val result = repository.getRecipeById("does-not-exist")

        assertTrue(result.isSuccess)
        assertNull(result.getOrThrow())
    }

    @Test
    fun `getRecipeById fails when the network call throws`() = runTest {
        coEvery { networkService.make<JsonObject>(any(), any(), any()) } throws IllegalStateException("offline")

        val result = repository.getRecipeById("1")

        assertTrue(result.isFailure)
    }
}
