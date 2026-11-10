package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearSubscriptionStore
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearProViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val store = mockk<WearSubscriptionStore>()
    private val wearableSync = mockk<WearableSyncService>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = WearProViewModel(store, wearableSync)

    @Test
    fun `emits null until the phone has published the state`() = runTest(dispatcher) {
        every { store.isPro } returns flowOf(null)
        val values = mutableListOf<Boolean?>()
        val job = launch { viewModel().isPro.collect { values += it } }

        assertEquals(listOf(null), values)
        job.cancel()
    }

    @Test
    fun `emits true once the phone publishes a PRO account`() = runTest(dispatcher) {
        every { store.isPro } returns flowOf(true)
        val values = mutableListOf<Boolean?>()
        val job = launch { viewModel().isPro.collect { values += it } }

        assertEquals(listOf(null, true), values)
        job.cancel()
    }

    @Test
    fun `opens the phone home when asked`() = runTest(dispatcher) {
        every { store.isPro } returns flowOf(false)

        viewModel().openPhone()

        coVerify { wearableSync.openPhoneApp("home") }
    }
}
