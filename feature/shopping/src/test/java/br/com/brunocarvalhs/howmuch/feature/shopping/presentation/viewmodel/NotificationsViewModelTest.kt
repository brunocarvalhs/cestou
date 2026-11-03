package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.repository.Notification
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationFilter
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.NotificationSectionKey
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.text.NotificationTexts
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.FormatNotificationTimeUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GroupNotificationsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.MarkAllNotificationsReadUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.NotificationType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    private val zone = ZoneId.of("UTC")
    private val now = Instant.parse("2026-11-02T12:00:00Z")
    private val clock = MutableClock(now, zone)

    private val testDispatcher = UnconfinedTestDispatcher()
    private val authService = mockk<AuthService>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val shoppingGetByIdUseCase = mockk<ShoppingGetByIdUseCase>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val texts = object : NotificationTexts {
        override fun justNow() = "now"
        override fun minutesAgo(minutes: Int) = "${minutes}m"
        override fun hoursAgo(hours: Int) = "${hours}h"
        override fun shortDate(date: LocalDate) = date.toString()
    }

    private val shopping = Shopping(
        id = "s1",
        title = "Weekly",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = listOf("u1", "u2"),
        roles = emptyMap()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { authService.currentUser } returns AuthenticatedUser(id = "u1")
        every { userRepository.getUserProfile(any()) } returns flowOf(null)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun notification(
        id: String,
        isRead: Boolean = false,
        type: String = "list_joined",
        ageMinutes: Long = 5
    ) = Notification(
        id = id, userId = "u1", title = "title $id", message = "msg $id", type = type, isRead = isRead,
        timestamp = now.minusSeconds(ageMinutes * 60).toEpochMilli(), shoppingId = "s1", senderId = "u2"
    )

    private fun viewModel(vararg items: Notification): NotificationsViewModel {
        every { notificationRepository.observeNotifications("u1") } returns flowOf(items.toList())
        return NotificationsViewModel(
            authService,
            notificationRepository,
            userRepository,
            shoppingGetByIdUseCase,
            GroupNotificationsUseCase(clock),
            FormatNotificationTimeUseCase(clock, texts),
            MarkAllNotificationsReadUseCase(notificationRepository),
            analyticsTracker,
            clock
        )
    }

    @Test
    fun `starts loading and tracks the screen view`() {
        every { authService.currentUser } returns null
        val vm = viewModel()

        verify { analyticsTracker.trackScreenView("notifications", "NotificationsViewModel") }
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `loads the feed into today section with relative time and typed items`() {
        val vm =
            viewModel(notification("n1", ageMinutes = 5), notification("n2", type = "list_finished", ageMinutes = 90))

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.hasUnread)
        val section = state.sections.single()
        assertEquals(NotificationSectionKey.TODAY, section.key)
        assertEquals(listOf("n1", "n2"), section.items.map { it.id })
        assertEquals("5m", section.items[0].relativeTime)
        assertEquals("1h", section.items[1].relativeTime)
        assertEquals(NotificationType.LIST_JOINED, section.items[0].type)
        assertEquals(NotificationType.LIST_FINISHED, section.items[1].type)
        assertEquals("s1", section.items[0].shoppingId)
    }

    @Test
    fun `unknown type maps to OTHER`() {
        val vm = viewModel(notification("n1", type = "promo"))

        assertEquals(NotificationType.OTHER, vm.uiState.value.sections.single().items.single().type)
    }

    @Test
    fun `hasUnread is false when everything is read`() {
        val vm = viewModel(notification("n1", isRead = true))

        assertFalse(vm.uiState.value.hasUnread)
    }

    @Test
    fun `no current user stops loading and shows nothing`() {
        every { authService.currentUser } returns null

        val vm = viewModel()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.sections.isEmpty())
    }

    @Test
    fun `empty feed leaves no sections`() {
        val vm = viewModel()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.sections.isEmpty())
    }

    @Test
    fun `lists filter hides unknown types and all brings them back`() {
        val vm = viewModel(notification("n1"), notification("n2", type = "promo"))

        vm.intent.onFilter(NotificationFilter.LISTS)
        assertEquals(NotificationFilter.LISTS, vm.uiState.value.filter)
        assertEquals(listOf("n1"), vm.uiState.value.sections.single().items.map { it.id })

        vm.intent.onFilter(NotificationFilter.ALL)
        assertEquals(2, vm.uiState.value.sections.single().items.size)
    }

    @Test
    fun `resolves each distinct sender profile once`() {
        every { userRepository.getUserProfile("u2") } returns flowOf(UserProfile(id = "u2", name = "Ana"))

        val vm = viewModel(notification("n1"), notification("n2"), notification("n3").copy(senderId = null))

        assertEquals("Ana", vm.uiState.value.senderProfiles["u2"]?.name)
        verify(exactly = 1) { userRepository.getUserProfile("u2") }
    }

    @Test
    fun `an unresolved sender profile leaves no entry`() {
        val vm = viewModel(notification("n1"))

        assertNull(vm.uiState.value.senderProfiles["u2"])
    }

    @Test
    fun `clicking an unread notification marks it read and opens its list`() = runTest {
        coEvery { notificationRepository.markAsRead("n1") } returns Result.success(Unit)
        coEvery { shoppingGetByIdUseCase("s1") } returns Result.success(shopping)
        val vm = viewModel(notification("n1"))
        var opened: Shopping? = null
        vm.onOpenShopping = { opened = it }

        vm.intent.onNotificationClick("n1")

        coVerify { notificationRepository.markAsRead("n1") }
        assertEquals(shopping, opened)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.NOTIFICATION_CLICKED,
                mapOf(AnalyticsParams.NOTIFICATION_TYPE to "list_joined")
            )
        }
    }

    @Test
    fun `clicking an already read notification does not write again but still opens the list`() = runTest {
        coEvery { shoppingGetByIdUseCase("s1") } returns Result.success(shopping)
        val vm = viewModel(notification("n1", isRead = true))
        var opened: Shopping? = null
        vm.onOpenShopping = { opened = it }

        vm.intent.onNotificationClick("n1")

        coVerify(exactly = 0) { notificationRepository.markAsRead(any()) }
        assertEquals(shopping, opened)
    }

    @Test
    fun `a legacy notification without shoppingId is only marked read`() = runTest {
        coEvery { notificationRepository.markAsRead("n1") } returns Result.success(Unit)
        val vm = viewModel(notification("n1").copy(shoppingId = null, senderId = null))
        var opened: Shopping? = null
        vm.onOpenShopping = { opened = it }

        vm.intent.onNotificationClick("n1")

        coVerify { notificationRepository.markAsRead("n1") }
        coVerify(exactly = 0) { shoppingGetByIdUseCase(any()) }
        assertNull(opened)
    }

    @Test
    fun `an unavailable list surfaces an error but the notification is still marked read`() = runTest {
        coEvery { notificationRepository.markAsRead("n1") } returns Result.success(Unit)
        coEvery { shoppingGetByIdUseCase("s1") } returns Result.failure(Exception("Shopping not found"))
        val vm = viewModel(notification("n1"))
        var opened: Shopping? = null
        vm.onOpenShopping = { opened = it }

        vm.intent.onNotificationClick("n1")

        coVerify { notificationRepository.markAsRead("n1") }
        assertNull(opened)
        assertEquals(R.string.notifications_error_list_unavailable, errorResId(vm))
    }

    @Test
    fun `a failed mark-as-read surfaces an error`() = runTest {
        coEvery { notificationRepository.markAsRead("n1") } returns Result.failure(Exception("offline"))
        val vm = viewModel(notification("n1").copy(shoppingId = null))

        vm.intent.onNotificationClick("n1")

        assertEquals(R.string.notifications_error_mark_read, errorResId(vm))
    }

    @Test
    fun `onErrorShown clears the error`() = runTest {
        coEvery { notificationRepository.markAsRead("n1") } returns Result.failure(Exception("offline"))
        val vm = viewModel(notification("n1").copy(shoppingId = null))
        vm.intent.onNotificationClick("n1")

        vm.intent.onErrorShown()

        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `clicking an unknown id does nothing`() = runTest {
        val vm = viewModel(notification("n1"))

        vm.intent.onNotificationClick("missing")

        coVerify(exactly = 0) { notificationRepository.markAsRead(any()) }
    }

    @Test
    fun `mark all read sends only unread ids and tracks the count`() = runTest {
        coEvery { notificationRepository.markAllAsRead(any()) } returns Result.success(Unit)
        val vm = viewModel(notification("a"), notification("b", isRead = true), notification("c"))

        vm.intent.onMarkAllRead()

        coVerify { notificationRepository.markAllAsRead(listOf("a", "c")) }
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.NOTIFICATIONS_MARK_ALL_READ,
                mapOf(AnalyticsParams.ITEMS_COUNT to 2)
            )
        }
    }

    @Test
    fun `mark all read with the lists filter leaves hidden notifications untouched`() = runTest {
        coEvery { notificationRepository.markAllAsRead(any()) } returns Result.success(Unit)
        val vm = viewModel(notification("a"), notification("hidden", type = "promo"))
        vm.intent.onFilter(NotificationFilter.LISTS)

        vm.intent.onMarkAllRead()

        coVerify { notificationRepository.markAllAsRead(listOf("a")) }
    }

    @Test
    fun `hasUnread only considers the visible notifications`() {
        val vm = viewModel(notification("read", isRead = true), notification("hidden", type = "promo"))

        assertTrue(vm.uiState.value.hasUnread)
        vm.intent.onFilter(NotificationFilter.LISTS)
        assertFalse(vm.uiState.value.hasUnread)
    }

    @Test
    fun `mark all read is limited to the feed cap`() = runTest {
        coEvery { notificationRepository.markAllAsRead(any()) } returns Result.success(Unit)
        val unread = (1..150).map { notification("n$it", ageMinutes = it.toLong()) }
        val vm = viewModel(*unread.toTypedArray())

        vm.intent.onMarkAllRead()

        coVerify {
            notificationRepository.markAllAsRead(match { it.size == GroupNotificationsUseCase.MAX_NOTIFICATIONS })
        }
    }

    @Test
    fun `changing the filter does not open another listener`() {
        val vm = viewModel(notification("a"))

        vm.intent.onFilter(NotificationFilter.LISTS)
        vm.intent.onFilter(NotificationFilter.ALL)

        verify(exactly = 1) { notificationRepository.observeNotifications("u1") }
    }

    @Test
    fun `mark all read with nothing unread does not call the repository`() = runTest {
        val vm = viewModel(notification("a", isRead = true))

        vm.intent.onMarkAllRead()

        coVerify(exactly = 0) { notificationRepository.markAllAsRead(any()) }
    }

    @Test
    fun `mark all read failure surfaces an error`() = runTest {
        coEvery { notificationRepository.markAllAsRead(any()) } returns Result.failure(Exception("offline"))
        val vm = viewModel(notification("a"))

        vm.intent.onMarkAllRead()

        assertEquals(R.string.notifications_error_mark_read, errorResId(vm))
    }

    @Test
    fun `onBack invokes the assigned callback`() {
        val vm = viewModel()
        var backCalled = false
        vm.onBack = { backCalled = true }

        vm.intent.onBack()

        assertTrue(backCalled)
    }

    private fun errorResId(vm: NotificationsViewModel): Int? =
        (vm.uiState.value.error as? br.com.brunocarvalhs.howmuch.core.ui.utils.UiText.StringResource)?.resId

    @Test
    fun `a second tap within the debounce window is ignored`() = runTest {
        coEvery { notificationRepository.markAsRead(any()) } returns Result.success(Unit)
        coEvery { shoppingGetByIdUseCase("s1") } returns Result.success(shopping)
        val vm = viewModel(notification("n1"), notification("n2"))
        var opened = 0
        vm.onOpenShopping = { opened++ }

        vm.intent.onNotificationClick("n1")
        vm.intent.onNotificationClick("n2")

        assertEquals(1, opened)
        coVerify(exactly = 1) { notificationRepository.markAsRead(any()) }
    }

    @Test
    fun `a tap after the debounce window is accepted`() = runTest {
        coEvery { notificationRepository.markAsRead(any()) } returns Result.success(Unit)
        coEvery { shoppingGetByIdUseCase("s1") } returns Result.success(shopping)
        val vm = viewModel(notification("n1"), notification("n2"))
        var opened = 0
        vm.onOpenShopping = { opened++ }

        vm.intent.onNotificationClick("n1")
        clock.advanceBy(Duration.ofSeconds(2))
        vm.intent.onNotificationClick("n2")

        assertEquals(2, opened)
    }

    @Test
    fun `refreshing the times re-renders the relative labels`() {
        val vm = viewModel(notification("n1", ageMinutes = 5))
        assertEquals("5m", vm.uiState.value.sections.single().items.single().relativeTime)

        clock.advanceBy(Duration.ofMinutes(10))
        vm.intent.onRefreshTimes()

        assertEquals("15m", vm.uiState.value.sections.single().items.single().relativeTime)
    }

    private class MutableClock(private var current: Instant, private val zoneId: ZoneId) : Clock() {
        fun advanceBy(duration: Duration) {
            current = current.plus(duration)
        }

        override fun getZone(): ZoneId = zoneId
        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)
        override fun instant(): Instant = current
    }
}
