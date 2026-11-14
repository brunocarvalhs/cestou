package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.MarkAllNotificationsReadUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearNotificationsIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearNotificationsViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val repository = mockk<NotificationRepository>()
    private val markAllRead = mockk<MarkAllNotificationsReadUseCase>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val notifications = MutableStateFlow<List<Notification>>(emptyList())
    private val unreadCount = MutableStateFlow(0)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { authService.currentUser } returns AuthenticatedUser(id = "user-1")
        every { repository.observeNotifications("user-1") } returns notifications
        every { repository.observeUnreadCount("user-1") } returns unreadCount
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun notification(id: String, isRead: Boolean) = Notification(
        id = id,
        userId = "user-1",
        title = "Lista $id",
        message = "Mensagem $id",
        type = "list_joined",
        isRead = isRead,
        timestamp = id.hashCode().toLong()
    )

    private fun viewModel() = WearNotificationsViewModel(authService, repository, markAllRead, analytics)

    @Test
    fun `shows the notifications and counts the unread ones`() {
        notifications.value = listOf(notification("a", isRead = false), notification("b", isRead = true))
        unreadCount.value = 1

        val state = viewModel().uiState.value

        assertEquals(2, state.items.items.size)
        assertEquals(1, state.unreadCount)
        assertEquals(false, state.isLoading)
        assertEquals(false, state.signedOut)
    }

    @Test
    fun `shows the empty state when there is no account on the watch`() {
        every { authService.currentUser } returns null

        val state = viewModel().uiState.value

        assertTrue(state.signedOut)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `marks only the unread notifications as read`() {
        val unread = notification("a", isRead = false)
        notifications.value = listOf(unread, notification("b", isRead = true))
        coEvery { markAllRead(any()) } returns Result.success(1)
        val vm = viewModel()

        vm.onIntent(WearNotificationsIntent.MarkAllRead)

        coVerify { markAllRead(listOf(unread)) }
    }

    @Test
    fun `does nothing on mark all when everything is already read`() {
        notifications.value = listOf(notification("a", isRead = true))
        val vm = viewModel()

        vm.onIntent(WearNotificationsIntent.MarkAllRead)

        coVerify(exactly = 0) { markAllRead(any()) }
    }

    @Test
    fun `tracks the screen open`() {
        viewModel()

        verify { analytics.trackEvent(AnalyticsEvents.WEAR_NOTIFICATIONS_OPEN, any()) }
    }
}
