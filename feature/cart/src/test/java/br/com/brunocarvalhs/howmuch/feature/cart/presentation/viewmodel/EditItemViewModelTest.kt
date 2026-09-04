package br.com.brunocarvalhs.howmuch.feature.cart.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class EditItemViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val useCase = mockk<ProductsUseCase>()
    private val authService = mockk<AuthService>()
    private val navigator = mockk<Navigator>(relaxed = true)

    private val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        .withActivity(ProductActivity.Action.ADDED, "user-a")

    private val savedStateHandle = SavedStateHandle(
        mapOf("product" to navJson.encodeToString(product), "shoppingId" to "list1")
    )
    private val viewModel = EditItemViewModel(savedStateHandle, useCase, authService)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel.setNavigator(navigator)
        coEvery { authService.getOrCreateUserId() } returns AuthenticatedUser(id = "user-b")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onSaveEdit updates price and quantity, then goes back`() = runTest {
        coEvery { useCase.update(any(), "list1") } returns Result.success(Unit)

        viewModel.onSaveEdit(product, price = 8.0, quantity = 3.0)

        coVerify {
            useCase.update(match { it.price == 8.0 && it.quantity == 3.0 }, "list1")
        }
        verify { navigator.goBack() }
    }

    @Test
    fun `onSaveEdit appends an EDITED entry for the current user on top of existing history`() = runTest {
        coEvery { useCase.update(any(), "list1") } returns Result.success(Unit)

        viewModel.onSaveEdit(product, price = 8.0, quantity = 3.0)

        coVerify {
            useCase.update(
                match {
                    it.history.map { entry -> entry.action } ==
                        listOf(ProductActivity.Action.ADDED, ProductActivity.Action.EDITED) &&
                        it.history.last().userId == "user-b"
                },
                "list1"
            )
        }
    }

    @Test
    fun `onSaveEdit does not navigate back when the update fails`() = runTest {
        coEvery { useCase.update(any(), "list1") } returns Result.failure(IllegalStateException("boom"))

        viewModel.onSaveEdit(product, price = 8.0, quantity = 3.0)

        verify(exactly = 0) { navigator.goBack() }
    }
}
