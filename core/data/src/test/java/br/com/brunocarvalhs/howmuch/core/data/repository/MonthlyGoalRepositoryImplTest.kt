package br.com.brunocarvalhs.howmuch.core.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.data.model.BudgetPreferencesModel
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KClass

class MonthlyGoalRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val authState = MutableStateFlow<AuthenticatedUser?>(AuthenticatedUser(id = "u1"))
    private val authService = mockk<AuthService> {
        every { this@mockk.authState } returns this@MonthlyGoalRepositoryImplTest.authState
        every { currentUser } answers { this@MonthlyGoalRepositoryImplTest.authState.value }
    }
    private val repository = MonthlyGoalRepositoryImpl(networkService, authService)

    @Test
    fun `observeGoal reads the private preferences document of the signed in user`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        every {
            networkService.observe<BudgetPreferencesModel>(capture(request), any(), any())
        } returns flowOf(BudgetPreferencesModel(id = "budget", monthlyGoal = 1500.0))

        repository.observeGoal().test {
            assertEquals(1500.0, awaitItem()!!, 1e-9)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("users/u1/preferences/budget", request.captured.endpoint)
        assertEquals(NetworkService.Method.GET, request.captured.method)
    }

    @Test
    fun `observeGoal emits null when the document does not exist`() = runTest {
        every { networkService.observe<BudgetPreferencesModel>(any(), any(), any()) } returns flowOf(null)

        repository.observeGoal().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeGoal treats a zero goal as no goal`() = runTest {
        every {
            networkService.observe<BudgetPreferencesModel>(any(), any(), any())
        } returns flowOf(BudgetPreferencesModel(monthlyGoal = 0.0))

        repository.observeGoal().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `observeGoal emits null without a session and never touches the network`() = runTest {
        authState.value = null

        repository.observeGoal().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { networkService.observe<BudgetPreferencesModel>(any(), any(), any()) }
    }

    @Test
    fun `observeGoal falls back to null when the stream fails`() = runTest {
        every {
            networkService.observe<BudgetPreferencesModel>(any(), any(), any())
        } returns flow { throw NetworkService.NetworkException(code = 500) }

        repository.observeGoal().test {
            assertNull(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setGoal writes only id and goal to the private document`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        coEvery { networkService.make(capture(request), any<KClass<String>>(), any()) } returns "budget"

        val result = repository.setGoal(1200.0)

        assertTrue(result.isSuccess)
        assertEquals(NetworkService.Method.POST, request.captured.method)
        assertEquals("users/u1/preferences", request.captured.endpoint)
        assertEquals(mapOf("id" to "budget", "monthlyGoal" to 1200.0), request.captured.payload)
    }

    @Test
    fun `setGoal never writes the goal into the public user document`() = runTest {
        val request = slot<NetworkService.NetworkRequest>()
        coEvery { networkService.make(capture(request), any<KClass<String>>(), any()) } returns "budget"

        repository.setGoal(1200.0)

        assertTrue(request.captured.endpoint != "users")
        assertTrue(request.captured.endpoint != "users/u1")
    }

    @Test
    fun `setGoal with null or zero clears the goal`() = runTest {
        val requests = mutableListOf<NetworkService.NetworkRequest>()
        coEvery { networkService.make(capture(requests), any<KClass<String>>(), any()) } returns "budget"

        repository.setGoal(null)
        repository.setGoal(0.0)

        assertEquals(2, requests.size)
        requests.forEach { assertEquals(mapOf("id" to "budget", "monthlyGoal" to null), it.payload) }
    }

    @Test
    fun `setGoal fails when the network returns nothing`() = runTest {
        coEvery { networkService.make(any(), any<KClass<String>>(), any()) } returns null

        assertTrue(repository.setGoal(500.0).isFailure)
    }

    @Test
    fun `setGoal fails without a session and writes nothing`() = runTest {
        authState.value = null

        assertTrue(repository.setGoal(500.0).isFailure)
        coVerify(exactly = 0) { networkService.make(any(), any<KClass<String>>(), any()) }
    }
}
