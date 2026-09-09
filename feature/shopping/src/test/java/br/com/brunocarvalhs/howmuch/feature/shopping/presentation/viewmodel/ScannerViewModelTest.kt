package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingJoinUseCase
import io.mockk.coEvery
import io.mockk.coVerify
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
class ScannerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val shoppingJoinUseCase = mockk<ShoppingJoinUseCase>()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = ScannerViewModel(shoppingJoinUseCase)

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
    fun `onTokenScanned joins the list and goes back on success`() = runTest {
        coEvery { shoppingJoinUseCase("ABC123") } returns Result.success(Unit)

        viewModel.intent.onTokenScanned("ABC123")

        verify { navigator.goBack() }
    }

    @Test
    fun `onTokenScanned does not navigate back when joining fails`() = runTest {
        coEvery { shoppingJoinUseCase("bad-token") } returns Result.failure(IllegalStateException("invalid"))

        viewModel.intent.onTokenScanned("bad-token")

        verify(exactly = 0) { navigator.goBack() }
    }

    @Test
    fun `onTokenScanned ignores repeated scans of the same code while a join is already in flight or succeeded`() =
        runTest {
            coEvery { shoppingJoinUseCase("ABC123") } returns Result.success(Unit)

            // Simulates BarcodeAnalyzer firing onBarcodeScanned repeatedly while the same QR code
            // stays in frame (see MVP-ROADMAP G13) — only the first frame should trigger a join.
            repeat(5) { viewModel.intent.onTokenScanned("ABC123") }

            coVerify(exactly = 1) { shoppingJoinUseCase("ABC123") }
            verify(exactly = 1) { navigator.goBack() }
        }

    @Test
    fun `onTokenScanned ignores repeated scans of different codes while a join is already in flight or succeeded`() =
        runTest {
            coEvery { shoppingJoinUseCase(any()) } returns Result.success(Unit)

            viewModel.intent.onTokenScanned("ABC123")
            viewModel.intent.onTokenScanned("XYZ789")
            viewModel.intent.onTokenScanned("QWE456")

            coVerify(exactly = 1) { shoppingJoinUseCase("ABC123") }
            coVerify(exactly = 0) { shoppingJoinUseCase("XYZ789") }
            coVerify(exactly = 0) { shoppingJoinUseCase("QWE456") }
            verify(exactly = 1) { navigator.goBack() }
        }

    @Test
    fun `onTokenScanned allows retrying with a new code after a failed join`() = runTest {
        coEvery { shoppingJoinUseCase("bad-token") } returns Result.failure(IllegalStateException("invalid"))
        coEvery { shoppingJoinUseCase("ABC123") } returns Result.success(Unit)

        viewModel.intent.onTokenScanned("bad-token")
        viewModel.intent.onTokenScanned("ABC123")

        coVerify(exactly = 1) { shoppingJoinUseCase("bad-token") }
        coVerify(exactly = 1) { shoppingJoinUseCase("ABC123") }
        verify(exactly = 1) { navigator.goBack() }
    }
}
