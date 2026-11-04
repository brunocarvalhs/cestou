package br.com.brunocarvalhs.howmuch.core.domain.budget

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class MonthlySpendingCalculatorTest {

    private val calculator = MonthlySpendingCalculator()
    private val utc = ZoneOffset.UTC
    private val november = Instant.parse("2026-11-15T12:00:00Z")

    private fun list(
        id: String,
        price: Double,
        status: Shopping.Status = Shopping.Status.FINISH,
        finishedAt: String? = null,
        createdAt: String = "2026-01-01T00:00:00Z"
    ) = Shopping(
        id = id,
        title = id,
        description = "",
        price = price,
        status = status,
        users = emptyList(),
        roles = emptyMap(),
        createdAt = Instant.parse(createdAt).toEpochMilli(),
        finishedAt = finishedAt?.let { Instant.parse(it).toEpochMilli() }
    )

    @Test
    fun `sums only finished lists of the reference month`() {
        val lists = listOf(
            list("a", 100.0, finishedAt = "2026-11-02T10:00:00Z"),
            list("b", 50.0, finishedAt = "2026-11-30T23:59:00Z"),
            list("old", 999.0, finishedAt = "2026-10-31T23:59:00Z"),
            list(
                "open",
                70.0,
                status = Shopping.Status.IN_PROGRESS,
                finishedAt = null,
                createdAt = "2026-11-05T00:00:00Z"
            )
        )

        assertEquals(150.0, calculator.finishedSpent(lists, november, utc), 1e-9)
    }

    @Test
    fun `a finished list without finishedAt counts in the month it was created`() {
        val lists = listOf(
            list("legacy", 80.0, finishedAt = null, createdAt = "2026-11-03T00:00:00Z"),
            list("legacy-old", 40.0, finishedAt = null, createdAt = "2026-09-03T00:00:00Z")
        )

        assertEquals(80.0, calculator.finishedSpent(lists, november, utc), 1e-9)
    }

    @Test
    fun `excludeId leaves the viewed list out`() {
        val lists = listOf(
            list("a", 100.0, finishedAt = "2026-11-02T10:00:00Z"),
            list("b", 50.0, finishedAt = "2026-11-03T10:00:00Z")
        )

        assertEquals(50.0, calculator.finishedSpent(lists, november, utc, excludeId = "a"), 1e-9)
    }

    @Test
    fun `the month boundary follows the given zone`() {
        // 2026-12-01T01:30Z is still 30 November in Sao Paulo (UTC-3).
        val saoPaulo = ZoneId.of("America/Sao_Paulo")
        val lists = listOf(list("edge", 60.0, finishedAt = "2026-12-01T01:30:00Z"))

        assertEquals(60.0, calculator.finishedSpent(lists, november, saoPaulo), 1e-9)
        assertEquals(0.0, calculator.finishedSpent(lists, november, utc), 1e-9)
    }

    @Test
    fun `no lists means zero`() {
        assertEquals(0.0, calculator.finishedSpent(emptyList(), november, utc), 1e-9)
    }

    @Test
    fun `reference month comes from the reference instant`() {
        val december = LocalDateTime.of(2026, 12, 10, 12, 0).toInstant(utc)
        val lists = listOf(list("nov", 100.0, finishedAt = "2026-11-02T10:00:00Z"))

        assertEquals(0.0, calculator.finishedSpent(lists, december, utc), 1e-9)
    }
}
