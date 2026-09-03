package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PairingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val wearableSyncService = mockk<WearableSyncService>()
    private val authToken = MutableSharedFlow<String>()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { wearableSyncService.generatePairingCode() } returns "123456"
        every { wearableSyncService.receivedAuthToken } returns authToken
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init generates a pairing code immediately`() {
        val viewModel = PairingViewModel(wearableSyncService)

        assertEquals("123456", viewModel.uiState.value.code)
        assertEquals(false, viewModel.uiState.value.isLinked)
    }

    @Test
    fun `receiving a non-empty auth token marks the device as linked`() = kotlinx.coroutines.test.runTest {
        val viewModel = PairingViewModel(wearableSyncService)

        authToken.emit("token-123")

        assertEquals(true, viewModel.uiState.value.isLinked)
    }

    @Test
    fun `an empty auth token does not mark the device as linked`() = kotlinx.coroutines.test.runTest {
        val viewModel = PairingViewModel(wearableSyncService)

        authToken.emit("")

        assertEquals(false, viewModel.uiState.value.isLinked)
    }
}
