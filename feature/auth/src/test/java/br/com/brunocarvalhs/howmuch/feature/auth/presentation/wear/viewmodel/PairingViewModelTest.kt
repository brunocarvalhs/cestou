package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val authState = MutableStateFlow<AuthenticatedUser?>(null)
    private val authService = mockk<AuthService> { every { authState } returns this@PairingViewModelTest.authState }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { wearableSyncService.generatePairingCode() } returns "123456"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init generates a pairing code immediately`() {
        val viewModel = PairingViewModel(wearableSyncService, authService)

        assertEquals("123456", viewModel.uiState.value.code)
        assertEquals(false, viewModel.uiState.value.isLinked)
    }

    @Test
    fun `the device is linked once the watch is signed in`() {
        val viewModel = PairingViewModel(wearableSyncService, authService)

        authState.value = AuthenticatedUser(id = "user-1")

        assertEquals(true, viewModel.uiState.value.isLinked)
    }

    @Test
    fun `signing out on the watch unlinks it`() {
        authState.value = AuthenticatedUser(id = "user-1")
        val viewModel = PairingViewModel(wearableSyncService, authService)

        authState.value = null

        assertEquals(false, viewModel.uiState.value.isLinked)
    }
}
