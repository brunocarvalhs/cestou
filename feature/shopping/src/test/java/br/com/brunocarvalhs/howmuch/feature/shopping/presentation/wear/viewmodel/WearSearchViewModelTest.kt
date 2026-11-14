package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.intent.WearSearchIntent
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WearSearchViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>()
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

    private fun shopping(id: String, title: String, status: Shopping.Status = Shopping.Status.NEW) = Shopping(
        id = id,
        title = title,
        description = "",
        price = 0.0,
        status = status,
        users = emptyList(),
        roles = emptyMap(),
        createdAt = id.hashCode().toLong()
    )

    private fun viewModel() = WearSearchViewModel(repository).also { vm ->
        CoroutineScope(dispatcher).launch { vm.uiState.collect {} }
    }

    @Test
    fun `empty query shows the whole list, active first then archived`() {
        lists.value = listOf(
            shopping("a", "Feira", Shopping.Status.FINISH),
            shopping("b", "Mercado")
        )

        val state = viewModel().uiState.value

        assertEquals(listOf("b"), state.active.items.map { it.id })
        assertEquals(listOf("a"), state.archived.items.map { it.id })
    }

    @Test
    fun `query filters by title ignoring case, in both groups`() {
        lists.value = listOf(
            shopping("a", "Feira do mês", Shopping.Status.FINISH),
            shopping("b", "Mercado da feira"),
            shopping("c", "Farmácia")
        )
        val vm = viewModel()

        vm.onIntent(WearSearchIntent.QueryChanged("FEIRA"))

        assertEquals("FEIRA", vm.uiState.value.query)
        assertEquals(listOf("b"), vm.uiState.value.active.items.map { it.id })
        assertEquals(listOf("a"), vm.uiState.value.archived.items.map { it.id })
    }

    @Test
    fun `query with no match leaves both groups empty`() {
        lists.value = listOf(shopping("b", "Mercado"))
        val vm = viewModel()

        vm.onIntent(WearSearchIntent.QueryChanged("xyz"))

        assertEquals(emptyList<String>(), vm.uiState.value.active.items.map { it.id })
    }
}
