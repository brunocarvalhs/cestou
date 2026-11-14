package br.com.brunocarvalhs.howmuch.update.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.remoteconfig.contract.RemoteVariableService
import br.com.brunocarvalhs.howmuch.core.remoteconfig.model.RemoteVariableKeys
import br.com.brunocarvalhs.howmuch.update.ForceUpdateController
import br.com.brunocarvalhs.howmuch.update.ForceUpdateState
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ForceUpdateViewModelTest {

    private val controller = mockk<ForceUpdateController>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)
    private val remoteVariables = mockk<RemoteVariableService> {
        every { getLong(RemoteVariableKeys.APP_MIN_VERSION_CODE, 0L) } returns 25L
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ForceUpdateViewModel(controller, remoteVariables, analyticsTracker)

    @Test
    fun `a required update is exposed and reported with both version codes`() = runTest {
        coEvery { controller.check(25L, any()) } returns ForceUpdateState.Required(resume = false)
        val viewModel = viewModel()

        viewModel.check()

        assertEquals(ForceUpdateState.Required(resume = false), viewModel.state.value)
        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.FORCE_UPDATE_REQUIRED,
                match {
                    it[AnalyticsParams.MIN_VERSION_CODE] == 25L && it[AnalyticsParams.INSTALLED_VERSION_CODE] is Long
                }
            )
        }
    }

    @Test
    fun `a blocked update is reported once even when checked again`() = runTest {
        coEvery { controller.check(any(), any()) } returns ForceUpdateState.Blocked
        val viewModel = viewModel()

        viewModel.check()
        viewModel.check()

        assertEquals(ForceUpdateState.Blocked, viewModel.state.value)
        verify(exactly = 1) { analyticsTracker.trackEvent(AnalyticsEvents.FORCE_UPDATE_BLOCKED, any()) }
    }
}
