package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.R
import br.com.brunocarvalhs.howmuch.feature.products.domain.exception.ImageUnreadableException
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductAnalyzeImageUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductDuplicateCheckUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel.ProductPhotoViewModel
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
    private val duplicateCheckUseCase = mockk<ProductDuplicateCheckUseCase>(relaxed = true)
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

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
        return ProductPhotoViewModel(
            context, savedStateHandle, analyzeImageUseCase, saveUseCase, duplicateCheckUseCase, analyticsTracker
        )
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
        coEvery { saveUseCase(product, "list1") } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onProductConfirmed(product)

        coVerify { saveUseCase(product, "list1") }
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_ADDED, any())
        }
    }

    @Test
    fun `onProductConfirmed shows the duplicate warning when the item is already on the list`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { saveUseCase(product, "list1") } returns Result.success(Unit)
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns product
        val vm = viewModel()

        vm.intent.onProductConfirmed(product)

        assertEquals(
            context.getString(R.string.quick_add_duplicate_warning, "Milk"),
            vm.uiState.value.confirmationMessage
        )
    }

    @Test
    fun `onCameraError shows the camera error and tracks the failure`() {
        val vm = viewModel()

        vm.intent.onCameraError()

        assertEquals(context.getString(R.string.product_photo_error_camera_failed), vm.uiState.value.errorMessage)
        verify {
            analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_PHOTO_SCAN_FAILED, any())
        }
    }

    @Test
    fun `onConfirmAllAnalysisItems notes how many items were already on the list`() = runTest {
        val product = Product(id = "p1", name = "Milk", quantity = 1.0, price = 5.0)
        coEvery { analyzeImageUseCase("content://photo") } returns Result.success(listOf(product))
        coEvery { duplicateCheckUseCase("Milk", "list1") } returns product
        coEvery { saveUseCase(product, "list1") } returns Result.success(Unit)
        val vm = viewModel()
        vm.intent.onImageCaptured(Uri.parse("content://photo"))
        vm.intent.onAnalyzeImage()

        vm.intent.onConfirmAllAnalysisItems()

        val expected = context.resources.getQuantityString(R.plurals.product_photo_items_added, 1, 1) +
            " " + context.resources.getQuantityString(R.plurals.product_photo_duplicate_items_warning, 1, 1)
        assertEquals(expected, vm.uiState.value.confirmationMessage)
    }

    @Test
    fun `onAnalyzeImage is a no-op when there is no captured image`() = runTest {
        val vm = viewModel()

        vm.intent.onAnalyzeImage()

        coVerify(exactly = 0) { analyzeImageUseCase(any()) }
    }

    @Test
    fun `onAnalyzeImage shows the load error when the image can't be read`() = runTest {
        coEvery { analyzeImageUseCase("content://photo") } returns Result.failure(ImageUnreadableException())
        val vm = viewModel()
        vm.intent.onImageCaptured(Uri.parse("content://photo"))

        vm.intent.onAnalyzeImage()

        assertEquals(context.getString(R.string.product_error_load_image), vm.uiState.value.errorMessage)
        assertEquals(false, vm.uiState.value.isAnalyzing)
        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_PHOTO_SCAN_FAILED, any()) }
    }

    @Test
    fun `onAnalyzeImage shows the analysis error and tracks it when analysis fails`() = runTest {
        coEvery { analyzeImageUseCase("content://photo") } returns Result.failure(IllegalStateException("boom"))
        val vm = viewModel()
        vm.intent.onImageCaptured(Uri.parse("content://photo"))

        vm.intent.onAnalyzeImage()

        assertEquals(context.getString(R.string.product_photo_error_analysis_failed), vm.uiState.value.errorMessage)
        verify { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_PHOTO_SCAN_FAILED, any()) }
    }
}
