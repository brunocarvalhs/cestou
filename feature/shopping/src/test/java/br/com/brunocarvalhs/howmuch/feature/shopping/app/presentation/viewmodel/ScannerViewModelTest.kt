package br.com.brunocarvalhs.howmuch.feature.shopping.app.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.app.domain.usecase.ShoppingJoinUseCase
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

        viewModel.onTokenScanned("ABC123")

        verify { navigator.goBack() }
    }

    @Test
    fun `onTokenScanned does not navigate back when joining fails`() = runTest {
        coEvery { shoppingJoinUseCase("bad-token") } returns Result.failure(IllegalStateException("invalid"))

        viewModel.onTokenScanned("bad-token")

        verify(exactly = 0) { navigator.goBack() }
    }
}
