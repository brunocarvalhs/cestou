package br.com.brunocarvalhs.howmuch.feature.products.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.Product
import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.UserProfile
import br.com.brunocarvalhs.howmuch.core.domain.model.withActivity
import br.com.brunocarvalhs.howmuch.core.domain.repository.UserRepository
import br.com.brunocarvalhs.howmuch.core.navigation.navJson
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductDuplicateCheckUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductSaveUseCase
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ProductsUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
@OptIn(ExperimentalCoroutinesApi::class)
class QuickAddViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val productsUseCase = mockk<ProductsUseCase>()
    private val productSaveUseCase = mockk<ProductSaveUseCase>(relaxed = true)
    private val productDuplicateCheckUseCase = mockk<ProductDuplicateCheckUseCase>()
    private val userRepository = mockk<UserRepository>()

    private fun shopping(budget: Double? = null) = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap(),
        budget = budget
    )

    private fun viewModel(shopping: Shopping = shopping()): QuickAddViewModel {
        val savedStateHandle = SavedStateHandle(mapOf("shopping" to navJson.encodeToString(shopping)))
        return QuickAddViewModel(
            context,
            savedStateHandle,
            productsUseCase,
            productSaveUseCase,
            productDuplicateCheckUseCase,
            userRepository
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
    fun `totalAmount matches the sum of Product total for the observed list`() = runTest {
        val products = listOf(
            Product(id = "p1", name = "Milk", quantity = 2.0, price = 5.0),
            Product(id = "p2", name = "Bread", quantity = 1.0, price = 8.5),
            Product(id = "p3", name = "Free sample", quantity = 3.0, price = null)
        )
        coEvery { productsUseCase("list1") } returns flowOf(products)

        val vm = viewModel()

        val expectedTotal = products.sumOf { it.total }
        assertEquals(expectedTotal, vm.uiState.value.totalAmount, 0.0)
    }

    @Test
    fun `isOverBudget reflects whether the total exceeds the shopping budget`() = runTest {
        coEvery { productsUseCase("list1") } returns
            flowOf(listOf(Product(id = "p1", name = "Milk", quantity = 1.0, price = 150.0)))
        assertTrue(viewModel(shopping(budget = 100.0)).uiState.value.isOverBudget)

        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        assertFalse(viewModel(shopping(budget = null)).uiState.value.isOverBudget)
    }

    @Test
    fun `onSubmit trims and saves a valid name, and is a no-op for a blank one`() = runTest {
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productDuplicateCheckUseCase("Arroz", "list1") } returns null
        coEvery { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") } returns
            Result.success(Unit)
        val vm = viewModel()

        vm.intent.onNewItemNameChange("  Arroz  ")
        vm.intent.onSubmit()

        coVerify { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") }
        assertEquals("", vm.uiState.value.newItemName)
        assertFalse(vm.uiState.value.isSaving)

        vm.intent.onNewItemNameChange("   ")
        vm.intent.onSubmit()

        // Still exactly one save — the blank-name submit was a no-op, not a second save.
        coVerify(exactly = 1) { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") }
    }

    @Test
    fun `onSubmit surfaces a save failure without clearing the field, clearable via onSaveErrorShown`() = runTest {
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productDuplicateCheckUseCase("Arroz", "list1") } returns null
        coEvery { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") } returns
            Result.failure(RuntimeException("network error"))
        val vm = viewModel()

        vm.intent.onNewItemNameChange("Arroz")
        vm.intent.onSubmit()

        // Field is NOT cleared and an error is surfaced — the user must not believe it was added.
        assertEquals("Arroz", vm.uiState.value.newItemName)
        assertTrue(vm.uiState.value.saveError!!.contains("Arroz"))
        assertFalse(vm.uiState.value.isSaving)

        vm.intent.onSaveErrorShown()

        assertNull(vm.uiState.value.saveError)
    }

    @Test
    fun `onSubmit ignores a second call while the first save is still in flight`() = runTest {
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productDuplicateCheckUseCase("Arroz", "list1") } returns null
        val saveGate = CompletableDeferred<Unit>()
        coEvery { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") } coAnswers {
            saveGate.await()
            Result.success(Unit)
        }
        val vm = viewModel()
        vm.intent.onNewItemNameChange("Arroz")

        vm.intent.onSubmit()
        assertTrue(vm.uiState.value.isSaving)
        vm.intent.onSubmit() // double-tap / Done-then-tap while the first save hasn't resolved yet

        saveGate.complete(Unit)

        coVerify(exactly = 1) { productSaveUseCase(name = "Arroz", quantity = 1.0, shoppingId = "list1") }
    }

    @Test
    fun `duplicate warning saves anyway, names a resolvable adder, and falls back otherwise`() = runTest {
        val existingWithAdder = Product(id = "p1", name = "Leite", quantity = 1.0)
            .withActivity(ProductActivity.Action.ADDED, userId = "user-a")
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productDuplicateCheckUseCase("leite", "list1") } returns existingWithAdder
        coEvery { productSaveUseCase(name = "leite", quantity = 1.0, shoppingId = "list1") } returns
            Result.success(Unit)

        coEvery { userRepository.getUserProfile("user-a") } returns
            flowOf(UserProfile(id = "user-a", name = "Ana"))
        val namedVm = viewModel()
        namedVm.intent.onNewItemNameChange("leite")
        namedVm.intent.onSubmit()
        // AC2: proceeding still saves the item normally, no hard block — and names the adder.
        coVerify { productSaveUseCase(name = "leite", quantity = 1.0, shoppingId = "list1") }
        assertTrue(namedVm.uiState.value.duplicateWarning!!.contains("Ana"))

        coEvery { userRepository.getUserProfile("user-a") } returns flowOf(null)
        val fallbackVm = viewModel()
        fallbackVm.intent.onNewItemNameChange("leite")
        fallbackVm.intent.onSubmit()
        assertFalse(fallbackVm.uiState.value.duplicateWarning!!.contains("user-a"))
        assertTrue(fallbackVm.uiState.value.duplicateWarning!!.contains("Leite"))
    }

    @Test
    fun `duplicate warning clears via onDuplicateWarningShown or onNewItemNameChange`() = runTest {
        val existing = Product(id = "p1", name = "Leite", quantity = 1.0)
        coEvery { productsUseCase("list1") } returns flowOf(emptyList())
        coEvery { productDuplicateCheckUseCase("leite", "list1") } returns existing
        coEvery { productSaveUseCase(name = "leite", quantity = 1.0, shoppingId = "list1") } returns
            Result.success(Unit)

        val vm = viewModel()
        vm.intent.onNewItemNameChange("leite")
        vm.intent.onSubmit()
        vm.intent.onDuplicateWarningShown()
        assertNull(vm.uiState.value.duplicateWarning)

        vm.intent.onNewItemNameChange("leite")
        vm.intent.onSubmit()
        vm.intent.onNewItemNameChange("Arroz")
        assertNull(vm.uiState.value.duplicateWarning)
    }
}
