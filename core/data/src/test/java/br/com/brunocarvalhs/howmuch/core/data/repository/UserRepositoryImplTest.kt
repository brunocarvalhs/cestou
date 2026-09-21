package br.com.brunocarvalhs.howmuch.core.data.repository

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.data.model.UserProfileModel
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserRepositoryImplTest {

    private val networkService = mockk<NetworkService>()
    private val repository = UserRepositoryImpl(networkService)

    @Test
    fun `getUserProfile maps network model to domain profile`() = runTest {
        val model = UserProfileModel(id = "u1", name = "Ana", email = "ana@test.com", photoUrl = null)
        every {
            networkService.observe<UserProfileModel>(any(), any(), any())
        } returns flowOf(model)

        repository.getUserProfile("u1").test {
            val profile = awaitItem()
            assertEquals("u1", profile?.id)
            assertEquals("Ana", profile?.name)
            awaitComplete()
        }
    }

    @Test
    fun `getUserProfile emits null when network returns null`() = runTest {
        every {
            networkService.observe<UserProfileModel>(any(), any(), any())
        } returns flowOf(null)

        repository.getUserProfile("u1").test {
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `updateProfile succeeds when network call succeeds`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val result = repository.updateProfile(UserProfile(id = "u1", name = "Ana", email = "ana@test.com"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `updateProfile fails when network call throws`() = runTest {
        coEvery {
            networkService.make<Boolean>(any(), any(), any())
        } throws NetworkService.NetworkException(code = 400)

        val result = repository.updateProfile(UserProfile(id = "u1", name = "Ana", email = "ana@test.com"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `deleteProfile succeeds when network call succeeds`() = runTest {
        coEvery { networkService.make<Boolean>(any(), any(), any()) } returns true

        val result = repository.deleteProfile("u1")

        assertTrue(result.isSuccess)
    }

    @Test
    fun `deleteProfile fails when network call throws`() = runTest {
        coEvery {
            networkService.make<Boolean>(any(), any(), any())
        } throws NetworkService.NetworkException(code = 404)

        val result = repository.deleteProfile("u1")

        assertTrue(result.isFailure)
    }
}
