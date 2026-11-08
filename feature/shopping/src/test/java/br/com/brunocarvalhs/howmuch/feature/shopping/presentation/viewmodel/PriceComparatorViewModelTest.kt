package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.ChainPriceIndex
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.SupermarketChain
import br.com.brunocarvalhs.howmuch.core.domain.model.UserLocation
import br.com.brunocarvalhs.howmuch.core.domain.repository.SupermarketRepository
import br.com.brunocarvalhs.howmuch.core.domain.usecase.CompareSupermarketPricesUseCase
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class PriceComparatorViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val repository = mockk<SupermarketRepository>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)

    private val location = UserLocation("RJ", "Niterói")
    private val cheap = SupermarketChain("cheap", "Cheap")
    private val pricey = SupermarketChain("pricey", "Pricey")
    private val shopping = Shopping(
        id = "list1",
        title = "Weekly",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap()
    )
    private val products = listOf(
        Product(id = "a", name = "Rice", quantity = 2.0, price = 10.0),
        Product(id = "b", name = "Beans", quantity = 1.0, price = null)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        coEvery { repository.getChainIndexes(location) } returns listOf(
            ChainPriceIndex(pricey, 1.1),
            ChainPriceIndex(cheap, 0.8)
        )
        coEvery { productsUseCase("list1") } returns flowOf(products)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = PriceComparatorViewModel(
        SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping))),
        productsUseCase,
        repository,
        CompareSupermarketPricesUseCase(repository, dispatcher),
        analytics
    )

    @Test
    fun `starts loading before the inputs arrive`() {
        every { repository.observeLocation() } returns flow { }

        assertTrue(viewModel().uiState.value.isLoading)
    }

    @Test
    fun `without a saved location it asks for one`() {
        every { repository.observeLocation() } returns flowOf(null)

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNull(state.comparison)
        assertTrue(state.showLocationForm)
    }

    @Test
    fun `with a location it ranks the chains and reports the comparison once`() {
        every { repository.observeLocation() } returns flowOf(location)

        val state = viewModel().uiState.value

        val comparison = assertNotNull(state.comparison).let { state.comparison!! }
        assertEquals(listOf(cheap, pricey), comparison.quotes.map { it.chain })
        assertEquals(1, comparison.unpricedCount)
        assertFalse(state.showLocationForm)
        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.PRICE_COMPARISON_EXECUTED, any()) }
    }

    @Test
    fun `a re-emission with the same total does not repeat the event`() {
        every { repository.observeLocation() } returns flowOf(location)
        val list = MutableStateFlow(products)
        coEvery { productsUseCase("list1") } returns list

        viewModel()
        list.value = products + Product(id = "c", name = "Salt", quantity = 1.0, price = null)

        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.PRICE_COMPARISON_EXECUTED, any()) }
    }

    @Test
    fun `a failure while comparing becomes an error state`() {
        every { repository.observeLocation() } returns flowOf(location)
        coEvery { repository.getChainIndexes(location) } throws IllegalStateException("boom")

        val state = viewModel().uiState.value

        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `a failure observing the inputs becomes an error state`() {
        every { repository.observeLocation() } returns flow { throw IllegalStateException("boom") }

        assertNotNull(viewModel().uiState.value.error)
    }

    @Test
    fun `changing the location reopens the form and is tracked`() {
        every { repository.observeLocation() } returns flowOf(location)
        val vm = viewModel()

        vm.intent.onChangeLocation()

        assertTrue(vm.uiState.value.showLocationForm)
        verify { analytics.trackEvent(AnalyticsEvents.PRICE_COMPARATOR_LOCATION_EDIT, any()) }
    }

    @Test
    fun `saving a location normalizes it and closes the form`() {
        every { repository.observeLocation() } returns flowOf(location)
        val vm = viewModel()
        vm.intent.onChangeLocation()

        vm.intent.onSaveLocation(" rj ", " Niterói ")

        coVerify { repository.saveLocation(UserLocation("RJ", "Niterói")) }
        verify { analytics.trackEvent(AnalyticsEvents.PRICE_COMPARATOR_LOCATION_SAVED, mapOf("state" to "RJ")) }
        assertFalse(vm.uiState.value.editingLocation)
    }

    @Test
    fun `saving a blank state or city is ignored`() {
        every { repository.observeLocation() } returns flowOf(null)
        val vm = viewModel()

        vm.intent.onSaveLocation("", "Niterói")
        vm.intent.onSaveLocation("RJ", "  ")

        coVerify(exactly = 0) { repository.saveLocation(any()) }
    }

    @Test
    fun `saving an unknown state is ignored`() {
        every { repository.observeLocation() } returns flowOf(null)
        val vm = viewModel()

        vm.intent.onSaveLocation("XX", "Niterói")

        coVerify(exactly = 0) { repository.saveLocation(any()) }
    }

    @Test
    fun `a failure opening the products flow becomes an error state`() {
        every { repository.observeLocation() } returns flowOf(location)
        coEvery { productsUseCase("list1") } throws IllegalStateException("boom")

        assertNotNull(viewModel().uiState.value.error)
    }

    @Test
    fun `a failure saving the location becomes an error and keeps the form open`() {
        every { repository.observeLocation() } returns flowOf(location)
        val vm = viewModel()
        vm.intent.onChangeLocation()
        coEvery { repository.saveLocation(any()) } throws java.io.IOException("disk")

        vm.intent.onSaveLocation("SP", "Santos")

        assertNotNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.editingLocation)
    }
}
