package br.com.brunocarvalhs.howmuch.feature.chat.presentation.viewmodel

import app.cash.turbine.test
import br.com.brunocarvalhs.howmuch.core.domain.model.SubscriptionStatus
import br.com.brunocarvalhs.howmuch.core.domain.repository.AiTrialRepository
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

class ChatAiGateViewModelTest {

    private val statusFlow = MutableSharedFlow<SubscriptionStatus>(replay = 1)
    private val usedFlow = MutableSharedFlow<Boolean>(replay = 1)
    private val subscriptionRepository = mockk<SubscriptionRepository>()
    private val aiTrialRepository = mockk<AiTrialRepository>()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { subscriptionRepository.observeStatus() } returns statusFlow
        every { aiTrialRepository.hasUsedFreeMessage() } returns usedFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ChatAiGateViewModel(subscriptionRepository, aiTrialRepository)

    @Test
    fun `Free user who has not used the trial can send`() = runTest {
        statusFlow.tryEmit(SubscriptionStatus.FREE)
        usedFlow.tryEmit(false)
        val viewModel = viewModel()

        viewModel.isSendEnabled.test {
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `Free user who already used the trial is blocked`() = runTest {
        statusFlow.tryEmit(SubscriptionStatus.FREE)
        usedFlow.tryEmit(true)
        val viewModel = viewModel()

        viewModel.isSendEnabled.test {
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `Pro user can send even after using the trial`() = runTest {
        statusFlow.tryEmit(SubscriptionStatus.PRO)
        usedFlow.tryEmit(true)
        val viewModel = viewModel()

        viewModel.isSendEnabled.test {
            assertEquals(true, awaitItem())
        }
    }
}
