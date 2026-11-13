package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import org.junit.Assert.assertEquals
import org.junit.Test

class WearListOrderTest {

    private fun list(
        id: String,
        createdAt: Long,
        status: Shopping.Status = Shopping.Status.NEW,
        isFavorite: Boolean = false,
        finishedAt: Long? = null
    ) = Shopping(
        id = id,
        title = id,
        description = "",
        price = 0.0,
        status = status,
        users = emptyList(),
        roles = emptyMap(),
        createdAt = createdAt,
        isFavorite = isFavorite,
        finishedAt = finishedAt
    )

    @Test
    fun `active lists are the most recently created first`() {
        val lists = listOf(list("old", 1), list("new", 3), list("mid", 2))

        assertEquals(listOf("new", "mid", "old"), lists.wearActiveLists().map { it.id })
    }

    @Test
    fun `pinned lists come before the rest, each group ordered by creation`() {
        val lists = listOf(
            list("plain-new", 5),
            list("pinned-old", 1, isFavorite = true),
            list("pinned-new", 4, isFavorite = true)
        )

        assertEquals(listOf("pinned-new", "pinned-old", "plain-new"), lists.wearActiveLists().map { it.id })
    }

    @Test
    fun `finished lists are left out of the active order`() {
        val lists = listOf(list("open", 1), list("done", 9, status = Shopping.Status.FINISH))

        assertEquals(listOf("open"), lists.wearActiveLists().map { it.id })
    }

    @Test
    fun `archived lists are the finished ones, most recent purchase first`() {
        val lists = listOf(
            list("bought-early", 1, status = Shopping.Status.FINISH, finishedAt = 10),
            list("bought-late", 2, status = Shopping.Status.FINISH, finishedAt = 30),
            list("open", 9)
        )

        assertEquals(listOf("bought-late", "bought-early"), lists.wearArchivedLists().map { it.id })
    }

    @Test
    fun `order depends only on creation and pin, never on opening (Q15)`() {
        // O modelo não guarda data de abertura: a ordem vem só de criação e fixada.
        val lists = listOf(list("a", 1), list("b", 2))

        assertEquals(listOf("b", "a"), lists.wearActiveLists().map { it.id })
    }
}
