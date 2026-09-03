package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShareShoppingUseCase
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
class ShareOptionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val shareShoppingUseCase = mockk<ShareShoppingUseCase>()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = ShareOptionsViewModel(shareShoppingUseCase)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap(),
        shortCode = "ABC123"
    )

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
    fun `onShareAsText shares the list then goes back`() = runTest {
        coEvery { shareShoppingUseCase(shopping) } returns Unit

        viewModel.onShareAsText(shopping)

        coVerify { shareShoppingUseCase(shopping) }
        verify { navigator.goBack() }
    }

    @Test
    fun `onInviteMember navigates using the short code when available`() {
        viewModel.onInviteMember(shopping)

        verify { navigator.goBack() }
        verify { navigator.navigate(QrCode("ABC123")) }
    }

    @Test
    fun `onInviteMember falls back to the shopping id when there is no short code`() {
        viewModel.onInviteMember(shopping.copy(shortCode = null))

        verify { navigator.navigate(QrCode("list1")) }
    }
}
