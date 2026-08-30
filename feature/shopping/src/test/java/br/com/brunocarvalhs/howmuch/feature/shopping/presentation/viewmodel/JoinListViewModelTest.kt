package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.navigation.mobile.Scanner
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JoinListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val shoppingJoinUseCase = mockk<ShoppingJoinUseCase>()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = JoinListViewModel(SavedStateHandle(), shoppingJoinUseCase)

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
    fun `onJoinByToken joins and goes back on success`() = runTest {
        coEvery { shoppingJoinUseCase("ABC123") } returns Result.success(Unit)

        viewModel.intent.onJoinByToken("ABC123")

        verify { navigator.goBack() }
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `onJoinByToken sets an error and does not navigate on failure`() = runTest {
        coEvery { shoppingJoinUseCase("bad-token") } returns Result.failure(IllegalStateException("invalid token"))

        viewModel.intent.onJoinByToken("bad-token")

        assertNotNull(viewModel.uiState.value.error)
        verify(exactly = 0) { navigator.goBack() }
    }

    @Test
    fun `onScanQrCode dismisses then navigates to the Scanner screen`() {
        viewModel.intent.onScanQrCode()

        verify { navigator.goBack() }
        verify { navigator.navigate(Scanner) }
    }

    @Test
    fun `onDismiss goes back`() {
        viewModel.intent.onDismiss()

        verify { navigator.goBack() }
    }
}
