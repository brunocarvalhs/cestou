package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
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
class FinishPurchaseViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>()
    private val navigator = mockk<Navigator>(relaxed = true)
    private val viewModel = FinishPurchaseViewModel(repository)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.IN_PROGRESS,
        users = emptyList(),
        roles = emptyMap()
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
    fun `onFinishPurchase updates price, establishment and FINISH status, then goes back`() = runTest {
        coEvery { repository.update(any()) } returns Unit

        viewModel.onFinishPurchase(shopping, price = 42.0, establishment = "Mercado X")

        coVerify {
            repository.update(
                match { it.price == 42.0 && it.description == "Mercado X" && it.status == Shopping.Status.FINISH }
            )
        }
        verify { navigator.goBack() }
    }
}
