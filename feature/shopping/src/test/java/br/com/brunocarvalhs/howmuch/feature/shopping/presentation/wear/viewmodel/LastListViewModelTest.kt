package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.viewmodel

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.domain.repository.ShoppingRepository
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListStore
import br.com.brunocarvalhs.howmuch.feature.shopping.data.local.WearListSummary
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LastListViewModelTest {

    private val store = mockk<WearListStore>()
    private val repository = mockk<ShoppingRepository>()
    private val viewModel = LastListViewModel(store, repository)

    private fun list(id: String, createdAt: Long, status: Shopping.Status = Shopping.Status.NEW) = Shopping(
        id = id, title = id, description = "", price = 0.0, status = status,
        users = emptyList(), roles = emptyMap(), createdAt = createdAt
    )

    @Test
    fun `reopens the last list without querying Firestore`() = runTest {
        coEvery { store.lastList() } returns WearListSummary("last", "Feira", total = 0.0, pending = 0, itemCount = 0)

        assertEquals("last", viewModel.lastListId())
        coVerify(exactly = 0) { repository.getAll() }
    }

    @Test
    fun `falls back to the newest unfinished list`() = runTest {
        coEvery { store.lastList() } returns null
        coEvery { repository.getAll() } returns listOf(
            list("old", createdAt = 1),
            list("done", createdAt = 3, status = Shopping.Status.FINISH),
            list("new", createdAt = 2)
        )

        assertEquals("new", viewModel.lastListId())
    }

    @Test
    fun `nothing to open when every list is finished`() = runTest {
        coEvery { store.lastList() } returns null
        coEvery { repository.getAll() } returns listOf(list("done", createdAt = 1, status = Shopping.Status.FINISH))

        assertNull(viewModel.lastListId())
    }
}
