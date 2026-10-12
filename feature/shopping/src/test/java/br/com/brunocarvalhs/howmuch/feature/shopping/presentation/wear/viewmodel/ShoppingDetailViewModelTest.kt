package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.usecase.ProductsUseCase
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingDetailViewModelTest {

    private val shopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        budget = 100.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap(),
        emoji = "🛒"
    )
    private val milk = Product(id = "p1", name = "Milk", quantity = 1.0, price = 10.0, isPurchased = true)
    private val bread = Product(id = "p2", name = "Bread", quantity = 2.0, price = 15.0)
    private val salt = Product(id = "p3", name = "Salt", quantity = 1.0)

    private val shoppingFlow = MutableStateFlow<Shopping?>(shopping)
    private val shoppingRepository = mockk<ShoppingRepository> {
        every { observeById("list1") } returns shoppingFlow
    }
    private val productsUseCase = mockk<ProductsUseCase>(relaxed = true) {
        coEvery { this@mockk.invoke("list1") } returns flowOf(listOf(milk, bread, salt))
    }
    private val authService = mockk<AuthService> {
        every { currentUser } returns AuthenticatedUser(id = "u1")
    }
    private val wearListStore = mockk<WearListStore>(relaxed = true)

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = ShoppingDetailViewModel(
        SavedStateHandle(mapOf("shoppingId" to "list1")),
        shoppingRepository,
        productsUseCase,
        authService,
        wearListStore
    )

    @Test
    fun `shows the total, budget left and pending items first`() {
        val state = viewModel().uiState.value

        assertEquals("Weekly Groceries", state.title)
        assertEquals(TOTAL, state.total, 0.0)
        assertEquals(REMAINING, state.remaining!!, 0.0)
        assertEquals(2, state.pending)
        assertEquals(listOf("p2", "p3", "p1"), state.items.items.map { it.id })
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `no budget means no remaining amount`() {
        shoppingFlow.value = shopping.copy(budget = null)

        assertNull(viewModel().uiState.value.remaining)
    }

    @Test
    fun `saves the list as the last one for the tile and the next launch`() {
        viewModel()

        coVerify {
            wearListStore.save(
                WearListSummary(
                    shoppingId = "list1",
                    title = "Weekly Groceries",
                    emoji = "🛒",
                    total = TOTAL,
                    budget = 100.0,
                    pending = 2,
                    itemCount = 3
                )
            )
        }
    }

    @Test
    fun `tapping a pending item marks it purchased and records who did it`() {
        val updated = slot<Product>()
        coEvery { productsUseCase.update(capture(updated), "list1") } returns Result.success(Unit)

        viewModel().intent.onToggle(bread)

        assertTrue(updated.captured.isPurchased)
        assertEquals(ProductActivity.Action.PURCHASED, updated.captured.history.last().action)
        assertEquals("u1", updated.captured.history.last().userId)
    }

    @Test
    fun `tapping a purchased item unmarks it`() {
        val updated = slot<Product>()
        coEvery { productsUseCase.update(capture(updated), "list1") } returns Result.success(Unit)

        viewModel().intent.onToggle(milk)

        assertEquals(false, updated.captured.isPurchased)
    }

    @Test
    fun `voice or keyboard text becomes a new item`() {
        val saved = slot<Product>()
        coEvery { productsUseCase.save(capture(saved), "list1") } returns Result.success(Unit)

        viewModel().intent.onAddItem("  leite condensado ")

        assertEquals("Leite condensado", saved.captured.name)
        assertEquals(1.0, saved.captured.quantity, 0.0)
        assertEquals(ProductActivity.Action.ADDED, saved.captured.history.single().action)
    }

    @Test
    fun `blank input adds nothing`() {
        viewModel().intent.onAddItem("   ")

        coVerify(exactly = 0) { productsUseCase.save(any(), any()) }
    }

    @Test
    fun `a list that is gone is forgotten and the screen goes back`() {
        val navigator = mockk<Navigator>(relaxed = true)
        coEvery { wearListStore.lastList() } returns
            WearListSummary("list1", "Weekly Groceries", total = 0.0, pending = 0, itemCount = 0)
        val vm = viewModel()
        vm.setNavigator(navigator)

        shoppingFlow.value = null

        coVerify { wearListStore.clear() }
        verify { navigator.goBack() }
    }
}

private const val TOTAL = 40.0

private const val REMAINING = 60.0
