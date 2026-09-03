package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.feature.products.domain.usecase.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.settings.domain.usecase.GetSettingsUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val repository = mockk<ShoppingRepository>(relaxed = true)
    private val shoppingGetAllUseCase = mockk<ShoppingGetAllUseCase>()
    private val shoppingGetByIdUseCase = mockk<ShoppingGetByIdUseCase>()
    private val shoppingCreateUseCase = mockk<ShoppingCreateUseCase>()
    private val shoppingUpdateUseCase = mockk<ShoppingUpdateUseCase>(relaxed = true)
    private val shoppingDuplicateUseCase = mockk<ShoppingDuplicateUseCase>(relaxed = true)
    private val shoppingDeleteUseCase = mockk<ShoppingDeleteUseCase>()
    private val shareShoppingUseCase = mockk<ShareShoppingUseCase>(relaxed = true)
    private val shoppingReopenUseCase = mockk<ShoppingReopenUseCase>(relaxed = true)
    private val getSettingsUseCase = mockk<GetSettingsUseCase>()
    private val authService = mockk<AuthService>()
    private val analyticsTracker = mockk<AnalyticsTracker>(relaxed = true)

    private val ownedShopping = Shopping(
        id = "list1",
        title = "Weekly Groceries",
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = listOf("owner-1"),
        roles = mapOf("owner-1" to User.Role.OWNER.name)
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repository.observeAll() } returns flowOf(listOf(ownedShopping))
        every { getSettingsUseCase() } returns flowOf(AppSettings())
        coEvery { shoppingGetAllUseCase() } returns Result.success(listOf(ownedShopping))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(currentUserId: String = "owner-1"): ShoppingListViewModel {
        every { authService.currentUser } returns AuthenticatedUser(id = currentUserId)
        return ShoppingListViewModel(
            repository,
            shoppingGetAllUseCase,
            shoppingGetByIdUseCase,
            shoppingCreateUseCase,
            shoppingUpdateUseCase,
            shoppingDuplicateUseCase,
            shoppingDeleteUseCase,
            shareShoppingUseCase,
            shoppingReopenUseCase,
            getSettingsUseCase,
            authService,
            analyticsTracker
        )
    }

    @Test
    fun `init tracks screen_view and loads the shopping list`() {
        val vm = viewModel()

        assertEquals(listOf(ownedShopping), vm.uiState.value.filteredList.items)
        coVerify { analyticsTracker.trackScreenView("shopping_list", "ShoppingListViewModel") }
    }

    @Test
    fun `onQueryChange filters the list by title`() {
        val vm = viewModel()

        vm.intent.onQueryChange("nothing matches")

        assertEquals(emptyList<Shopping>(), vm.uiState.value.filteredList.items)
    }

    @Test
    fun `onCreateConfirmed creates a list and tracks the created event`() = runTest {
        val created = ownedShopping.copy(id = "new-list")
        coEvery {
            shoppingCreateUseCase.invoke("Title", "Desc", Shopping.DEFAULT_EMOJI)
        } returns Result.success(created)
        val vm = viewModel()

        vm.intent.onCreateConfirmed("Title", "Desc", Shopping.DEFAULT_EMOJI)

        coVerify {
            analyticsTracker.trackEvent(AnalyticsEvents.SHOPPING_LIST_CREATED, mapOf("shopping_id" to "new-list"))
        }
    }

    @Test
    fun `onDelete deletes and tracks the event when the current user is the owner`() = runTest {
        coEvery { repository.getById("list1") } returns ownedShopping
        coEvery { shoppingDeleteUseCase("list1") } returns Result.success(Unit)
        val vm = viewModel(currentUserId = "owner-1")

        vm.intent.onDelete("list1")

        coVerify { shoppingDeleteUseCase("list1") }
        coVerify {
            analyticsTracker.trackEvent(AnalyticsEvents.SHOPPING_LIST_DELETED, mapOf("shopping_id" to "list1"))
        }
    }

    @Test
    fun `onDelete sets an error and does not delete when the current user is not the owner`() = runTest {
        coEvery { repository.getById("list1") } returns ownedShopping
        val vm = viewModel(currentUserId = "someone-else")

        vm.intent.onDelete("list1")

        coVerify(exactly = 0) { shoppingDeleteUseCase(any()) }
        assertNotNull(vm.uiState.value.error)
    }

    @Test
    fun `onShare shares the list and tracks the event`() = runTest {
        val vm = viewModel()

        vm.intent.onShare(ownedShopping)

        coVerify { shareShoppingUseCase(ownedShopping) }
        coVerify {
            analyticsTracker.trackEvent(AnalyticsEvents.SHOPPING_LIST_SHARED, mapOf("shopping_id" to "list1"))
        }
    }

    @Test
    fun `onToggleFavorite flips the favorite flag`() = runTest {
        coEvery { shoppingUpdateUseCase("list1", ownedShopping.copy(isFavorite = true)) } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onToggleFavorite(ownedShopping)

        coVerify { shoppingUpdateUseCase("list1", ownedShopping.copy(isFavorite = true)) }
    }

    @Test
    fun `onShowCreateSheet toggles the create sheet visibility`() {
        val vm = viewModel()

        vm.intent.onShowCreateSheet(true)

        assertEquals(true, vm.uiState.value.isCreateSheetVisible)
    }
}
