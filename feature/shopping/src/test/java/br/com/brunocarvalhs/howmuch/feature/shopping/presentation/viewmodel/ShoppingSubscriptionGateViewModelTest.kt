package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.SubscriptionRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ShoppingSubscriptionGateViewModelTest {

    private val statusFlow = MutableSharedFlow<SubscriptionStatus>(replay = 1)
    private val subscriptionRepository = mockk<SubscriptionRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { subscriptionRepository.observeStatus() } returns statusFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `Free user is not Pro`() = runTest {
        statusFlow.tryEmit(SubscriptionStatus.FREE)
        val viewModel = ShoppingSubscriptionGateViewModel(subscriptionRepository)

        viewModel.isPro.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `Pro user is Pro`() = runTest {
        statusFlow.tryEmit(SubscriptionStatus.PRO)
        val viewModel = ShoppingSubscriptionGateViewModel(subscriptionRepository)

        viewModel.isPro.test {
            assertEquals(true, awaitItem())
        }
    }
}
