package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.viewmodel

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.domain.model.AppSettings
import br.com.brunocarvalhs.howmuch.core.domain.model.AuthenticatedUser
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.model.User
import br.com.brunocarvalhs.howmuch.core.domain.repository.SettingsRepository
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.core.domain.services.AuthService
import br.com.brunocarvalhs.howmuch.core.domain.services.ShareShoppingUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingCreateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingDeleteUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingDuplicateUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetAllUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingGetByIdUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingReopenUseCase
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.ShoppingUpdateUseCase
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
import org.junit.Before
import org.junit.Test

/**
 * Split out of [ShoppingListViewModelTest] (TooManyFunctions) to cover the two row-level quick
 * actions that write a single field via [ShoppingUpdateUseCase]: favoriting and quick-finish.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelQuickActionsTest {

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
    private val settingsRepository = mockk<SettingsRepository>()
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
        every { settingsRepository.getSettings() } returns flowOf(AppSettings())
        every { authService.currentUser } returns AuthenticatedUser(id = "owner-1")
        coEvery { shoppingGetAllUseCase() } returns Result.success(listOf(ownedShopping))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(): ShoppingListViewModel = ShoppingListViewModel(
        repository,
        shoppingGetAllUseCase,
        shoppingGetByIdUseCase,
        shoppingCreateUseCase,
        shoppingUpdateUseCase,
        shoppingDuplicateUseCase,
        shoppingDeleteUseCase,
        shareShoppingUseCase,
        shoppingReopenUseCase,
        settingsRepository,
        authService,
        analyticsTracker
    )

    @Test
    fun `onToggleFavorite flips the favorite flag`() = runTest {
        coEvery { shoppingUpdateUseCase("list1", mapOf("isFavorite" to true)) } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onToggleFavorite(ownedShopping)

        coVerify { shoppingUpdateUseCase("list1", mapOf("isFavorite" to true)) }
    }

    @Test
    fun `onUpdate from the quick-finish row action writes only the status field`() = runTest {
        coEvery { shoppingUpdateUseCase("list1", mapOf("status" to "FINISH")) } returns Result.success(Unit)
        val vm = viewModel()

        vm.intent.onUpdate(ownedShopping.copy(status = Shopping.Status.FINISH))

        coVerify { shoppingUpdateUseCase("list1", mapOf("status" to "FINISH")) }
    }
}
