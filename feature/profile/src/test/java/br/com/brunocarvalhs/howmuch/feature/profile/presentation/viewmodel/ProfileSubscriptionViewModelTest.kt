package br.com.brunocarvalhs.howmuch.feature.profile.presentation.viewmodel

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

class ProfileSubscriptionViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `exposes the repository's current status once collected`() = runTest {
        val statusFlow = MutableSharedFlow<SubscriptionStatus>(replay = 1)
        statusFlow.tryEmit(SubscriptionStatus.PRO)
        val subscriptionRepository = mockk<SubscriptionRepository>()
        every { subscriptionRepository.observeStatus() } returns statusFlow

        val viewModel = ProfileSubscriptionViewModel(subscriptionRepository)

        viewModel.status.test {
            assertEquals(SubscriptionStatus.PRO, awaitItem())
        }
    }

    @Test
    fun `defaults to FREE before the repository emits`() = runTest {
        val statusFlow = MutableSharedFlow<SubscriptionStatus>(replay = 1)
        val subscriptionRepository = mockk<SubscriptionRepository>()
        every { subscriptionRepository.observeStatus() } returns statusFlow

        val viewModel = ProfileSubscriptionViewModel(subscriptionRepository)

        viewModel.status.test {
            assertEquals(SubscriptionStatus.FREE, awaitItem())
        }
    }
}
