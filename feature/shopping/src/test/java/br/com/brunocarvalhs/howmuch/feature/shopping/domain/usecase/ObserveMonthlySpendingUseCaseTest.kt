package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.toSpending
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class ObserveMonthlySpendingUseCaseTest {

    private val observeHistory = mockk<ObserveHistoryUseCase>()
    private val useCase = ObserveMonthlySpendingUseCase(observeHistory)

    @Test
    fun `keeps only the six most recent months in ascending order`() = runTest {
        every { observeHistory() } returns flowOf((1..8).map { historyMonth(2026, it, "p$it") })

        val result = useCase().first()

        assertEquals((3..8).map { YearMonth.of(2026, it) }, result.map { it.key })
    }

    @Test
    fun `fewer months than the limit are all kept`() = runTest {
        every { observeHistory() } returns flowOf(listOf(historyMonth(2026, 11, "a")))

        assertEquals(1, useCase().first().size)
    }

    @Test
    fun `a month converts to a spending point with its purchase count`() {
        val spending = historyMonth(2026, 11, "a", "b").toSpending()

        assertEquals(YearMonth.of(2026, 11), spending.key)
        assertEquals(200.0, spending.total, 0.0)
        assertEquals(2, spending.purchaseCount)
    }
}
