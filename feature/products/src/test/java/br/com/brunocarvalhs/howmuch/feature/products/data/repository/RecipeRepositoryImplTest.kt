package br.com.brunocarvalhs.howmuch.feature.products.data.repository

import br.com.brunocarvalhs.howmuch.core.common.BuildConfig
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import com.google.ai.client.generativeai.GenerativeModel
import io.mockk.coEvery
import io.mockk.every
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
    private val remoteVariableService = mockk<RemoteVariableService>()
    private val repository = RecipeRepositoryImpl(networkService, remoteVariableService)

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

    @Test
    fun `generativeModel is built with the remote key when Remote Config returns a valid value`() {
        every {
            remoteVariableService.getString(
                key = RemoteVariableKeys.GEMINI_API_KEY,
                default = BuildConfig.GEMINI_API_KEY
            )
        } returns "remote-rotated-key"

        assertEquals("remote-rotated-key", repository.actualGeminiApiKey())
    }

    @Test
    fun `generativeModel falls back to the BuildConfig key when Remote Config has no value`() {
        // Mirrors FirebaseRemoteConfigService's real fallback contract: an unfetched/unactivated
        // key returns the caller-supplied default.
        every {
            remoteVariableService.getString(
                key = RemoteVariableKeys.GEMINI_API_KEY,
                default = BuildConfig.GEMINI_API_KEY
            )
        } returns BuildConfig.GEMINI_API_KEY

        assertEquals(BuildConfig.GEMINI_API_KEY, repository.actualGeminiApiKey())
    }

    @Test
    fun `generativeModel falls back to the BuildConfig key when Remote Config returns a blank value`() {
        every {
            remoteVariableService.getString(
                key = RemoteVariableKeys.GEMINI_API_KEY,
                default = BuildConfig.GEMINI_API_KEY
            )
        } returns "   "

        assertEquals(BuildConfig.GEMINI_API_KEY, repository.actualGeminiApiKey())
    }

    /** Forces the private `generativeModel by lazy` and reads the key it was actually built with. */
    private fun RecipeRepositoryImpl.actualGeminiApiKey(): String {
        val field = RecipeRepositoryImpl::class.java.getDeclaredField("generativeModel\$delegate")
        field.isAccessible = true
        val lazyModel = field.get(this) as Lazy<*>
        return (lazyModel.value as GenerativeModel).apiKey
    }
}
