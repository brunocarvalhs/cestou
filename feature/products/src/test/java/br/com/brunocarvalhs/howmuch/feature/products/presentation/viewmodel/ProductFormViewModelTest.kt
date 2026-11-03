package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.service.FirstTimeEventTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductDuplicateCheckUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.presentation.helper.ProductAddSession
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
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ProductFormViewModelTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val saveUseCase = mockk<ProductSaveUseCase>()
    private val duplicateCheck = mockk<ProductDuplicateCheckUseCase>(relaxed = true)
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

    private val addSession = ProductAddSession()

    private fun viewModel() = ProductFormViewModel(
        context,
        SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping))),
        saveUseCase,
        duplicateCheck,
        mockk(relaxed = true),
        analyticsTracker,
        mockk<FirstTimeEventTracker>(relaxed = true),
        addSession
    )

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `closing without adding a product reports product_add_abandoned`() {
        viewModel().reportAbandonment()

        verify {
            analyticsTracker.trackEvent(
                AnalyticsEvents.PRODUCT_ADD_ABANDONED,
                mapOf(AnalyticsParams.SHOPPING_ID to "list1", AnalyticsParams.SOURCE to "form")
            )
        }
    }

    @Test
    fun `closing after adding a product does not report abandonment`() = runTest {
        coEvery { saveUseCase(any(), any()) } returns Result.success(Unit)
        coEvery { duplicateCheck(any(), any()) } returns null
        val viewModel = viewModel()
        viewModel.intent.onNameChange("Arroz")
        viewModel.intent.onQuantityChange("1")
        viewModel.intent.onSave()

        viewModel.reportAbandonment()

        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_ADD_ABANDONED, any()) }
    }

    @Test
    fun `adding a product through another path of the picker is not an abandonment`() {
        val viewModel = viewModel()
        addSession.onProductAdded()

        viewModel.reportAbandonment()

        verify(exactly = 0) { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_ADD_ABANDONED, any()) }
    }

    @Test
    fun `two view models closing together report the abandonment once`() {
        val viewModel = viewModel()

        viewModel.reportAbandonment()
        viewModel.reportAbandonment()

        verify(exactly = 1) { analyticsTracker.trackEvent(AnalyticsEvents.PRODUCT_ADD_ABANDONED, any()) }
    }
}
