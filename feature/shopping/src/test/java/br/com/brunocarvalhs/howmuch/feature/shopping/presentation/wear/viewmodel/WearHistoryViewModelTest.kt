package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase.GroupHistoryByMonthUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class WearHistoryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = mockk<ShoppingRepository>()
    private val lists = MutableStateFlow<List<Shopping>>(emptyList())
    private val clock = Clock.fixed(Instant.parse("2026-11-10T12:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.observeAll() } returns lists
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun finished(id: String, finishedAt: Instant, price: Double) = Shopping(
        id = id,
        title = id,
        description = "",
        price = price,
        status = Shopping.Status.FINISH,
        users = emptyList(),
        roles = emptyMap(),
        finishedAt = finishedAt.toEpochMilli()
    )

    private fun viewModel() = WearHistoryViewModel(repository, GroupHistoryByMonthUseCase(clock)).also { vm ->
        CoroutineScope(dispatcher).launch { vm.uiState.collect {} }
    }

    @Test
    fun `months come from the phone grouping, most recent month first`() {
        lists.value = listOf(
            finished("out", Instant.parse("2026-10-05T12:00:00Z"), 40.0),
            finished("nov-a", Instant.parse("2026-11-02T12:00:00Z"), 10.0),
            finished("nov-b", Instant.parse("2026-11-08T12:00:00Z"), 15.0)
        )

        val state = viewModel().uiState.value

        assertEquals(listOf("2026-11", "2026-10"), state.months.items.map { it.key.toString() })
        assertEquals(25.0, state.months.items.first().total, 0.0)
        assertFalse(state.isLoading)
    }

    @Test
    fun `no finished lists means no months`() {
        lists.value = emptyList()

        assertEquals(emptyList<String>(), viewModel().uiState.value.months.items.map { it.key.toString() })
    }
}
