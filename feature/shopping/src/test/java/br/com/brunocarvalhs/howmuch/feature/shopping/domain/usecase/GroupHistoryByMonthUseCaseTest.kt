package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class GroupHistoryByMonthUseCaseTest {

    private val useCase = GroupHistoryByMonthUseCase(TestClock)

    @Test
    fun `groups finished lists by month and sums their prices`() {
        val result = useCase(
            listOf(
                finishedShopping("a", price = 10.0, finishedAt = millisOf(2026, 10, 3)),
                finishedShopping("b", price = 15.5, finishedAt = millisOf(2026, 10, 20)),
                finishedShopping("c", price = 7.0, finishedAt = millisOf(2026, 11, 1))
            )
        )

        assertEquals(listOf(YearMonth.of(2026, 10), YearMonth.of(2026, 11)), result.map { it.key })
        assertEquals(25.5, result[0].total, 0.0)
        assertEquals(7.0, result[1].total, 0.0)
    }

    @Test
    fun `months are oldest first and purchases newest first`() {
        val result = useCase(
            listOf(
                finishedShopping("nov", finishedAt = millisOf(2026, 11, 5)),
                finishedShopping("sep-early", finishedAt = millisOf(2026, 9, 2)),
                finishedShopping("sep-late", finishedAt = millisOf(2026, 9, 28))
            )
        )

        assertEquals(listOf(YearMonth.of(2026, 9), YearMonth.of(2026, 11)), result.map { it.key })
        assertEquals(listOf("sep-late", "sep-early"), result[0].purchases.map { it.shopping.id })
    }

    @Test
    fun `ignores lists that are not finished`() {
        val result = useCase(
            listOf(
                finishedShopping("new", status = Shopping.Status.NEW, finishedAt = millisOf(2026, 11, 1)),
                finishedShopping("wip", status = Shopping.Status.IN_PROGRESS, finishedAt = millisOf(2026, 11, 1))
            )
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `finishedAt wins over createdAt`() {
        val result = useCase(
            listOf(
                finishedShopping("a", createdAt = millisOf(2026, 8, 1), finishedAt = millisOf(2026, 11, 2))
            )
        )

        assertEquals(YearMonth.of(2026, 11), result.single().key)
    }

    @Test
    fun `falls back to createdAt when finishedAt is missing`() {
        val result = useCase(listOf(finishedShopping("legacy", createdAt = millisOf(2026, 8, 15))))

        assertEquals(YearMonth.of(2026, 8), result.single().key)
        assertEquals(millisOf(2026, 8, 15), result.single().purchases.single().date)
    }

    @Test
    fun `a purchase with price zero still shows up and adds nothing`() {
        val result = useCase(listOf(finishedShopping("free", price = 0.0, finishedAt = millisOf(2026, 11, 2))))

        assertEquals(1, result.single().purchases.size)
        assertEquals(0.0, result.single().total, 0.0)
    }

    @Test
    fun `december and january land in different years`() {
        val result = useCase(
            listOf(
                finishedShopping("jan", finishedAt = millisOf(2027, 1, 2)),
                finishedShopping("dec", finishedAt = millisOf(2026, 12, 30))
            )
        )

        assertEquals(listOf(YearMonth.of(2026, 12), YearMonth.of(2027, 1)), result.map { it.key })
    }

    @Test
    fun `uses the clock zone so 23h30 on the last day stays in that month`() {
        val result = useCase(listOf(finishedShopping("late", finishedAt = millisOf(2026, 10, 31, 23, 30))))

        assertEquals(YearMonth.of(2026, 10), result.single().key)
    }

    @Test
    fun `people count is the member count with a minimum of one`() {
        val result = useCase(
            listOf(
                finishedShopping("solo", finishedAt = millisOf(2026, 11, 1)).copy(users = emptyList()),
                finishedShopping("trio", finishedAt = millisOf(2026, 11, 2)).copy(users = listOf("a", "b", "c"))
            )
        )

        val counts = result.single().purchases.associate { it.shopping.id to it.peopleCount }
        assertEquals(mapOf("solo" to 1, "trio" to 3), counts)
    }

    @Test
    fun `blank establishment becomes null`() {
        val result = useCase(
            listOf(finishedShopping("a", finishedAt = millisOf(2026, 11, 1)).copy(establishment = "  "))
        )

        assertEquals(null, result.single().purchases.single().establishment)
    }

    @Test
    fun `empty input gives no months`() {
        assertTrue(useCase(emptyList()).isEmpty())
    }
}
