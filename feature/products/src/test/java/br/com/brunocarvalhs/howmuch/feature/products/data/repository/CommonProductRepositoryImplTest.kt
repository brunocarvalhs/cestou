package br.com.brunocarvalhs.howmuch.feature.products.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.feature.products.data.model.CommonProductModel
import br.com.brunocarvalhs.howmuch.feature.products.domain.model.CommonProduct
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CommonProductRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val authService = mockk<AuthService>()
    private val repository = CommonProductRepositoryImpl(networkService, authService)

    private val model = CommonProductModel(id = "1", name = "Arroz", category = "Mercearia", unit = "kg")

    @Test
    fun `getAll emits the mapped list for the resolved user`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        every {
            networkService.observe<List<CommonProductModel>>(any(), any(), any())
        } returns flowOf(listOf(model))

        repository.getAll().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Arroz", result[0].name)
            awaitComplete()
        }
    }

    @Test
    fun `getAll emits an empty list when the network returns null`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        every {
            networkService.observe<List<CommonProductModel>>(any(), any(), any())
        } returns flowOf(null)

        repository.getAll().test {
            assertEquals(emptyList<CommonProduct>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `seedDefaultsIfEmpty seeds the starter set when the user has nothing yet`() = runTest {
        // Both calls share the same erased make(request, KClass, KType) signature, so the mock
        // must discriminate by request.method (GET check vs POST seed), not by reified type.
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery {
            networkService.make<List<CommonProductModel>>(
                match { it.method == NetworkService.Method.GET },
                any(),
                any()
            )
        } returns emptyList()
        coEvery {
            networkService.make<String>(match { it.method == NetworkService.Method.POST }, any(), any())
        } returns "generated-id"

        val result = repository.seedDefaultsIfEmpty()

        assertEquals(true, result.isSuccess)
        coVerify(atLeast = 15) {
            networkService.make<String>(match { it.method == NetworkService.Method.POST }, any(), any())
        }
    }

    @Test
    fun `seedDefaultsIfEmpty does nothing when the user already has items`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery {
            networkService.make<List<CommonProductModel>>(
                match { it.method == NetworkService.Method.GET },
                any(),
                any()
            )
        } returns listOf(model)

        val result = repository.seedDefaultsIfEmpty()

        assertEquals(true, result.isSuccess)
        coVerify(exactly = 0) {
            networkService.make<String>(match { it.method == NetworkService.Method.POST }, any(), any())
        }
    }

    @Test
    fun `seedDefaultsIfEmpty fails when user resolution throws`() = runTest {
        coEvery { authService.getOrCreateUserId() } throws IllegalStateException("no session")

        val result = repository.seedDefaultsIfEmpty()

        assertEquals(true, result.isFailure)
    }

    @Test
    fun `add sends the mapped payload for the resolved user`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery { networkService.make<String>(any(), any(), any()) } returns "1"

        val result = repository.add(CommonProduct(id = "1", name = "Arroz"))

        assertEquals(true, result.isSuccess)
    }

    @Test
    fun `remove calls the network with the product id`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val result = repository.remove("1")

        assertEquals(true, result.isSuccess)
    }

    @Test
    fun `remove fails when the network call throws`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery { networkService.make<Boolean>(any(), any(), any()) } throws IllegalStateException("offline")

        val result = repository.remove("1")

        assertEquals(true, result.isFailure)
    }
}
