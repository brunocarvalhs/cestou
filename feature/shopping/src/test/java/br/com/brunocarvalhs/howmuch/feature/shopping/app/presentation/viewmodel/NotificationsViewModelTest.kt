package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads notifications for the current user`() {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { notificationRepository.observeNotifications("u1") } returns flowOf(
            listOf(
                Notification(
                    id = "n1",
                    userId = "u1",
                    title = "Lista compartilhada",
                    message = "msg",
                    type = "SHARE",
                    isRead = false,
                    timestamp = 1000L
                )
            )
        )

        val vm = NotificationsViewModel(authService, notificationRepository)

        assertEquals(1, vm.uiState.value.notifications.size)
        assertEquals("n1", vm.uiState.value.notifications.first().id)
    }

    @Test
    fun `init does nothing when there is no current user`() {
        every { authService.currentUser } returns null

        val vm = NotificationsViewModel(authService, notificationRepository)

        assertEquals(emptyList<Any>(), vm.uiState.value.notifications)
    }

    @Test
    fun `onNotificationClick marks the notification as read`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { notificationRepository.observeNotifications("u1") } returns flowOf(emptyList())
        val vm = NotificationsViewModel(authService, notificationRepository)

        vm.intent.onNotificationClick("n1")

        coVerify { notificationRepository.markAsRead("n1") }
    }

    @Test
    fun `onBack invokes the assigned callback`() {
        every { authService.currentUser } returns null
        val vm = NotificationsViewModel(authService, notificationRepository)
        var backCalled = false
        vm.onBack = { backCalled = true }

        vm.intent.onBack()

        assertEquals(true, backCalled)
    }
}
