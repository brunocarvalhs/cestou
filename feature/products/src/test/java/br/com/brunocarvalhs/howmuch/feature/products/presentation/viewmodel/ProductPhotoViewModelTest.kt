package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductAnalyzeImageUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.app.presentation.viewmodel.ProductPhotoViewModel
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductPhotoViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val analyzeImageUseCase = mockk<ProductAnalyzeImageUseCase>()
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

    private fun viewModel(): ProductPhotoViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return ProductPhotoViewModel(context, savedStateHandle, analyzeImageUseCase, saveUseCase)
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
    fun `onRetake clears the captured image and analysis state`() {
        val vm = viewModel()

        vm.intent.onRetake()

        assertNull(vm.uiState.value.capturedImageUri)
        assertEquals(emptyList<Product>(), vm.uiState.value.analysisResult)
    }

    @Test
    fun `onProductConfirmed saves the product into the current shopping list`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        val vm = viewModel()

        vm.intent.onProductConfirmed(product)

        coVerify { saveUseCase(product, "list1") }
    }

    @Test
    fun `onAnalyzeImage is a no-op when there is no captured image`() = runTest {
        val vm = viewModel()

        vm.intent.onAnalyzeImage()

        coVerify(exactly = 0) { analyzeImageUseCase(any()) }
    }
}
