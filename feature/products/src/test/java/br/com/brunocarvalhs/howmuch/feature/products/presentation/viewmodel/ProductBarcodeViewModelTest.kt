package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductGetByBarcodeUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.ProductBarcodeViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductBarcodeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val getByBarcodeUseCase = mockk<ProductGetByBarcodeUseCase>()
    private val saveUseCase = mockk<ProductSaveUseCase>(relaxed = true)

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )

    private fun viewModel(): ProductBarcodeViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return ProductBarcodeViewModel(savedStateHandle, getByBarcodeUseCase, saveUseCase)
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
    fun `onBarcodeScanner saves the found product into the current shopping list`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0, barcode = "789")
        coEvery { getByBarcodeUseCase("789") } returns Result.success(product)

        viewModel().intent.onBarcodeScanner("789")

        coVerify { saveUseCase(product, "list1") }
    }

    @Test
    fun `onBarcodeScanner sets an error message when the lookup fails`() = runTest {
        coEvery { getByBarcodeUseCase("789") } returns Result.failure(IllegalStateException("not found"))

        val vm = viewModel()
        vm.intent.onBarcodeScanner("789")

        assertEquals("not found", vm.uiState.value.errorMessage)
    }

    @Test
    fun `onFlashToggle flips the flash state`() {
        val vm = viewModel()

        vm.intent.onFlashToggle()

        assertTrue(vm.uiState.value.isFlashOn)
    }
}
