package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.common.wearable.WearableSyncService
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.WEAR_HOME_RECENT_LIMIT
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearHomeIntent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearHomeViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>()
    private val wearableSyncService = mockk<WearableSyncService>()
    private val lists = MutableStateFlow<List<Shopping>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.observeAll() } returns lists
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun shopping(id: String, createdAt: Long, isFavorite: Boolean = false) = Shopping(
        id = id,
        title = id,
        description = "",
        price = 0.0,
        status = Shopping.Status.NEW,
        users = emptyList(),
        roles = emptyMap(),
        createdAt = createdAt,
        isFavorite = isFavorite
    )

    /** `uiState` é `WhileSubscribed`: sem um coletor, `value` fica no inicial. Este coletor mantém a leitura. */
    private fun viewModel() = WearHomeViewModel(repository, wearableSyncService).also { vm ->
        CoroutineScope(dispatcher).launch { vm.uiState.collect {} }
    }

    @Test
    fun `home shows at most five recent lists and offers more`() {
        lists.value = (1..7).map { shopping("l$it", createdAt = it.toLong()) }

        val state = viewModel().uiState.value

        assertEquals(WEAR_HOME_RECENT_LIMIT, state.lists.items.size)
        assertEquals("l7", state.lists.items.first().id)
        assertTrue(state.hasMore)
        assertFalse(state.showAll)
        assertFalse(state.isLoading)
    }

    @Test
    fun `no more button when there are five lists or fewer`() {
        lists.value = (1..3).map { shopping("l$it", createdAt = it.toLong()) }

        val state = viewModel().uiState.value

        assertEquals(3, state.lists.items.size)
        assertFalse(state.hasMore)
    }

    @Test
    fun `toggle show all reveals every active list and hides them again`() {
        lists.value = (1..7).map { shopping("l$it", createdAt = it.toLong()) }
        val vm = viewModel()

        vm.onIntent(WearHomeIntent.ToggleShowAll)
        assertEquals(7, vm.uiState.value.lists.items.size)
        assertTrue(vm.uiState.value.showAll)

        vm.onIntent(WearHomeIntent.ToggleShowAll)
        assertEquals(WEAR_HOME_RECENT_LIMIT, vm.uiState.value.lists.items.size)
    }

    @Test
    fun `pinned lists come first in the recent five`() {
        lists.value = (1..6).map { shopping("l$it", createdAt = it.toLong(), isFavorite = it == 1) }

        val ids = viewModel().uiState.value.lists.items.map { it.id }

        assertEquals("l1", ids.first())
    }

    @Test
    fun `create on phone opens the phone create route`() {
        coEvery { wearableSyncService.openPhoneApp("shopping/new") } returns true
        val vm = viewModel()

        vm.onIntent(WearHomeIntent.CreateListOnPhone)

        coVerify { wearableSyncService.openPhoneApp("shopping/new") }
        assertFalse(vm.uiState.value.phoneUnavailable)
    }

    @Test
    fun `phone unavailable is shown when the phone cannot be reached`() {
        coEvery { wearableSyncService.openPhoneApp(any()) } returns false
        val vm = viewModel()

        vm.onIntent(WearHomeIntent.CreateListOnPhone)

        assertTrue(vm.uiState.value.phoneUnavailable)
    }

    @Test
    fun `a failing repository shows an empty home instead of crashing`() {
        every { repository.observeAll() } returns flow { throw IllegalStateException("offline") }

        val state = viewModel().uiState.value

        assertTrue(state.lists.items.isEmpty())
        assertFalse(state.hasMore)
    }
}
