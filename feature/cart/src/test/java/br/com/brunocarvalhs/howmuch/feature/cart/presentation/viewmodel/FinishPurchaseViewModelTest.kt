package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackMoment
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationOrigin
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.FeedbackPromptRequester
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.ui.utils.UiText
import br.com.brunocarvalhs.howmuch.feature.cart.R
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FinishPurchaseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<ShoppingRepository>()
    private val authService = mockk<AuthService>()
    private val notificationRepository = mockk<NotificationRepository>(relaxed = true)
    private val navigator = mockk<Navigator>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val feedbackPromptRequester = mockk<FeedbackPromptRequester>(relaxed = true)
    private val viewModel =
        FinishPurchaseViewModel(
            context,
            repository,
            authService,
            notificationRepository,
            analyticsTracker,
            mockk(relaxed = true),
            feedbackPromptRequester
        )

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "Original description",
        price = 0.0,
        status = Shopping.Status.IN_PROGRESS,
        users = listOf("u1", "u2"),
        roles = emptyMap()
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel.setNavigator(navigator)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onFinishPurchase writes only status, price, establishment and finishedAt, then goes back`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1", displayName = "Ana")
        val fields = slot<Map<String, Any?>>()
        coEvery { repository.updateFields("list1", capture(fields)) } returns Result.success(Unit)
        val before = System.currentTimeMillis()

        viewModel.onFinishPurchase(shopping, price = PRICE, establishment = "Mercado X")

        assertEquals(setOf("status", "price", "establishment", "finishedAt"), fields.captured.keys)
        assertEquals("FINISH", fields.captured["status"])
        assertEquals(PRICE, fields.captured["price"])
        assertEquals("Mercado X", fields.captured["establishment"])
        assertTrue((fields.captured["finishedAt"] as Long) >= before)
        verify { navigator.goBack() }
        verify { feedbackPromptRequester.onMoment(FeedbackMoment.PURCHASE_FINISHED) }
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onFinishPurchase notifies the other members but not the actor`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1", displayName = "Ana")
        coEvery { repository.updateFields(any(), any()) } returns Result.success(Unit)

        viewModel.onFinishPurchase(shopping, price = PRICE, establishment = "Mercado X")

        coVerify {
            notificationRepository.notify(
                "u2",
                any(),
                any(),
                "list_finished",
                NotificationOrigin(shoppingId = "list1", senderId = "u1")
            )
        }
        coVerify(exactly = 0) { notificationRepository.notify("u1", any(), any(), any(), any()) }
    }

    @Test
    fun `onFinishPurchase does not navigate or notify when the write fails`() = runTest {
        coEvery { repository.updateFields(any(), any()) } returns Result.failure(IllegalStateException("offline"))

        viewModel.onFinishPurchase(shopping, price = PRICE, establishment = "Mercado X")

        verify(exactly = 0) { navigator.goBack() }
        verify(exactly = 0) { feedbackPromptRequester.onMoment(any()) }
        coVerify(exactly = 0) { notificationRepository.notify(any(), any(), any(), any(), any()) }
        assertEquals(false, viewModel.uiState.value.isLoading)
        val error = viewModel.uiState.value.error
        assertTrue(error is UiText.StringResource)
        assertEquals(R.string.cart_error_finish_purchase, (error as UiText.StringResource).resId)
    }
}

private const val PRICE = 42.0
