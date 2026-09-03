package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import android.content.Context
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.NotificationRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
    private val viewModel = FinishPurchaseViewModel(context, repository, authService, notificationRepository)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
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
    fun `onFinishPurchase updates price, establishment and FINISH status, then goes back`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1", displayName = "Ana")
        coEvery { repository.update(any()) } returns Unit

        viewModel.onFinishPurchase(shopping, price = 42.0, establishment = "Mercado X")

        coVerify {
            repository.update(
                match { it.price == 42.0 && it.description == "Mercado X" && it.status == Shopping.Status.FINISH }
            )
        }
        verify { navigator.goBack() }
    }

    @Test
    fun `onFinishPurchase notifies the other members but not the actor`() = runTest {
        every { authService.currentUser } returns AuthenticatedUser(id = "u1", displayName = "Ana")
        coEvery { repository.update(any()) } returns Unit

        viewModel.onFinishPurchase(shopping, price = 42.0, establishment = "Mercado X")

        coVerify { notificationRepository.notify("u2", any(), any(), "list_finished") }
        coVerify(exactly = 0) { notificationRepository.notify("u1", any(), any(), any()) }
    }
}
