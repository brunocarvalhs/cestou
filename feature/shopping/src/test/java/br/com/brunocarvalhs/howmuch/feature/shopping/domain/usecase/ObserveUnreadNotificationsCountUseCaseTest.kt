package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveUnreadNotificationsCountUseCaseTest {

    private val authService = mockk<AuthService>()
    private val repository = mockk<NotificationRepository>()
    private val useCase = ObserveUnreadNotificationsCountUseCase(authService, repository)

    @Test
    fun `emits the repository count for the signed-in user`() = runTest {
        every { authService.authState } returns MutableStateFlow(AuthenticatedUser(id = "u1"))
        every { repository.observeUnreadCount("u1") } returns flowOf(3, 2)

        useCase().test {
            assertEquals(3, awaitItem())
            assertEquals(2, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits zero without querying when nobody is signed in`() = runTest {
        every { authService.authState } returns MutableStateFlow(null)

        useCase().test {
            assertEquals(0, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        verify(exactly = 0) { repository.observeUnreadCount(any()) }
    }

    @Test
    fun `starts counting once the user signs in after the flow was collected`() = runTest {
        val auth = MutableStateFlow<AuthenticatedUser?>(null)
        every { authService.authState } returns auth
        every { repository.observeUnreadCount("u1") } returns MutableStateFlow(4)

        useCase().test {
            assertEquals(0, awaitItem())
            auth.value = AuthenticatedUser(id = "u1")
            assertEquals(4, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switches to the new account when the user changes`() = runTest {
        val auth = MutableStateFlow<AuthenticatedUser?>(AuthenticatedUser(id = "u1"))
        every { authService.authState } returns auth
        every { repository.observeUnreadCount("u1") } returns MutableStateFlow(1)
        every { repository.observeUnreadCount("u2") } returns MutableStateFlow(7)

        useCase().test {
            assertEquals(1, awaitItem())
            auth.value = AuthenticatedUser(id = "u2")
            assertEquals(7, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
