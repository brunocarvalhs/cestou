package br.com.brunocarvalhs.howmuch.feature.shopping.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.model.ShoppingModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoppingRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val authService = mockk<AuthService>()
    private val repository = ShoppingRepositoryImpl(networkService, authService)

    private val model = ShoppingModel(
        id = "s1",
        title = "Weekly Groceries",
        description = "desc",
        price = 10.0,
        status = Shopping.Status.NEW,
        users = listOf("user-1"),
        roles = mapOf("user-1" to "OWNER")
    )

    @Test
    fun `observeAll emits mapped list when user resolves`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        every { networkService.observe<List<ShoppingModel>>(any(), any(), any()) } returns flowOf(listOf(model))

        repository.observeAll().test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("s1", result[0].id)
            awaitComplete()
        }
    }

    @Test
    fun `observeAll emits empty list when network returns null`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        every { networkService.observe<List<ShoppingModel>>(any(), any(), any()) } returns flowOf(null)

        repository.observeAll().test {
            assertEquals(emptyList<Shopping>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeAll emits empty list without querying network when user id cannot be resolved`() = runTest {
        coEvery { authService.getOrCreateUserId() } throws IllegalStateException("no session")

        repository.observeAll().test {
            assertEquals(emptyList<Shopping>(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observeById emits mapped shopping`() = runTest {
        every { networkService.observe<ShoppingModel>(any(), any(), any()) } returns flowOf(model)

        repository.observeById("s1").test {
            assertEquals("s1", awaitItem()?.id)
            awaitComplete()
        }
    }

    @Test
    fun `observeById emits null when network returns null`() = runTest {
        every { networkService.observe<ShoppingModel>(any(), any(), any()) } returns flowOf(null)

        repository.observeById("s1").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getAll returns mapped list when user resolves`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery { networkService.make<List<ShoppingModel>>(any(), any(), any()) } returns listOf(model)

        val result = repository.getAll()

        assertEquals(1, result.size)
        assertEquals("s1", result[0].id)
    }

    @Test
    fun `getAll returns empty list when user id cannot be resolved`() = runTest {
        coEvery { authService.getOrCreateUserId() } throws IllegalStateException("no session")

        val result = repository.getAll()

        assertEquals(emptyList<Shopping>(), result)
    }

    @Test
    fun `getAll returns empty list when network returns null`() = runTest {
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-1")
        coEvery { networkService.make<List<ShoppingModel>>(any(), any(), any()) } returns null

        val result = repository.getAll()

        assertEquals(emptyList<Shopping>(), result)
    }

    @Test
    fun `getById returns mapped shopping`() = runTest {
        coEvery { networkService.make<ShoppingModel>(any(), any(), any()) } returns model

        val result = repository.getById("s1")

        assertEquals("s1", result?.id)
    }

    @Test
    fun `getById returns null when network returns null`() = runTest {
        coEvery { networkService.make<ShoppingModel>(any(), any(), any()) } returns null

        val result = repository.getById("s1")

        assertNull(result)
    }

    @Test
    fun `create sends the mapped payload`() = runTest {
        coEvery { networkService.make<String>(any(), any(), any()) } returns "s1"

        repository.create(model.toDomainForTest())
    }

    @Test
    fun `update sends the mapped payload`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        repository.update(model.toDomainForTest())
    }

    @Test
    fun `delete calls network with the shopping id`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        repository.delete(model.toDomainForTest())
    }

    @Test
    fun `join adds the user to the shopping list`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        repository.join("s1", "user-2")
    }

    @Test
    fun `getByShortCode returns the first mapped match`() = runTest {
        coEvery { networkService.make<List<ShoppingModel>>(any(), any(), any()) } returns listOf(model)

        val result = repository.getByShortCode("ABC123")

        assertEquals("s1", result?.id)
    }

    @Test
    fun `getByShortCode returns null when nothing matches`() = runTest {
        coEvery { networkService.make<List<ShoppingModel>>(any(), any(), any()) } returns emptyList()

        val result = repository.getByShortCode("ABC123")

        assertNull(result)
    }

    @Test
    fun `getByShortCode returns null when network returns null`() = runTest {
        coEvery { networkService.make<List<ShoppingModel>>(any(), any(), any()) } returns null

        val result = repository.getByShortCode("ABC123")

        assertNull(result)
    }

    @Test
    fun `updatePositions succeeds`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val result = repository.updatePositions(listOf(model.toDomainForTest()))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `updatePositions writes each shopping's position`() = runTest {
        val requests = mutableListOf<NetworkService.NetworkRequest>()
        coEvery { networkService.make<Boolean>(capture(requests), any(), any()) } returns true

        val shoppings = listOf(
            model.copy(id = "s1", position = 0).toDomainForTest(),
            model.copy(id = "s2", position = 1).toDomainForTest()
        )

        repository.updatePositions(shoppings)

        assertEquals(2, requests.size)
        assertTrue(requests.any { it.endpoint == "shopping/s1" && it.payload == mapOf("position" to 0) })
        assertTrue(requests.any { it.endpoint == "shopping/s2" && it.payload == mapOf("position" to 1) })
    }

    @Test
    fun `updatePositions fails when network throws`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } throws NetworkService.NetworkException(message = "offline")

        val result = repository.updatePositions(listOf(model.toDomainForTest()))

        assertTrue(result.isFailure)
    }

    private fun ShoppingModel.toDomainForTest(): Shopping = Shopping(
        id = id,
        title = title,
        description = description,
        price = price,
        status = status,
        users = users,
        roles = roles,
        position = position
    )
}
