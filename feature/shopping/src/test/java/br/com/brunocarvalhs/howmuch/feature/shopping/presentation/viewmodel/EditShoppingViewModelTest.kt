package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.QrCode
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class EditShoppingViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>()
    private val shoppingUpdateUseCase = mockk<ShoppingUpdateUseCase>()
    private val navigator = mockk<Navigator>(relaxed = true)

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

    private fun viewModel(): EditShoppingViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return EditShoppingViewModel(savedStateHandle, repository, shoppingUpdateUseCase).also {
            it.setNavigator(navigator)
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads the shopping list from the route`() {
        val vm = viewModel()

        assertEquals(shopping, vm.uiState.value.shopping)
    }

    @Test
    fun `onUpdate updates the shopping list then goes back`() = runTest {
        val updated = shopping.copy(title = "New Title")
        coEvery { shoppingUpdateUseCase("list1", updated) } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onUpdate(updated)

        verify { navigator.goBack() }
        assertEquals(false, vm.uiState.value.isLoading)
    }

    @Test
    fun `onUpdate sets an error message when the update fails`() = runTest {
        coEvery { shoppingUpdateUseCase("list1", shopping) } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()

        vm.intent.onUpdate(shopping)

        assertEquals("boom", vm.uiState.value.error)
    }

    @Test
    fun `onShareToken navigates using the shopping's short code`() = runTest {
        coEvery { repository.getById("list1") } returns shopping
        val vm = viewModel()

        vm.intent.onShareToken("list1")

        coVerify { navigator.navigate(QrCode("ABC123")) }
    }
}
