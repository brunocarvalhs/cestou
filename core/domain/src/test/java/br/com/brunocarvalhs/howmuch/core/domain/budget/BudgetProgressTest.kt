package br.com.brunocarvalhs.howmuch.core.domain.budget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetProgressTest {

    @Test
    fun `no limit means no ratio, no remaining and no level`() {
        val progress = BudgetProgress(limit = null, spent = 50.0)

        assertNull(progress.ratio)
        assertNull(progress.remaining)
        assertEquals(BudgetLevel.NONE, progress.level)
    }

    @Test
    fun `a zero or negative limit is ignored like no limit`() {
        assertEquals(BudgetLevel.NONE, BudgetProgress(limit = 0.0, spent = 10.0).level)
        assertEquals(BudgetLevel.NONE, BudgetProgress(limit = -5.0, spent = 10.0).level)
    }

    @Test
    fun `below 80 percent is ok`() {
        val progress = BudgetProgress(limit = 100.0, spent = 79.9)

        assertEquals(BudgetLevel.OK, progress.level)
        assertEquals(20.1, progress.remaining!!, 1e-9)
    }

    @Test
    fun `exactly 80 percent is a warning`() {
        assertEquals(BudgetLevel.WARNING, BudgetProgress(limit = 100.0, spent = 80.0).level)
    }

    @Test
    fun `floating point sums do not push 80 percent below the warning line`() {
        val spent = 0.1 + 0.2 + 0.3 + 0.2 // 0.8 with floating point drift
        assertEquals(BudgetLevel.WARNING, BudgetProgress(limit = 1.0, spent = spent).level)
    }

    @Test
    fun `between 80 and 100 percent stays a warning`() {
        assertEquals(BudgetLevel.WARNING, BudgetProgress(limit = 100.0, spent = 99.9).level)
    }

    @Test
    fun `exactly 100 percent is exceeded with nothing left`() {
        val progress = BudgetProgress(limit = 100.0, spent = 100.0)

        assertEquals(BudgetLevel.EXCEEDED, progress.level)
        assertEquals(0.0, progress.remaining!!, 1e-9)
    }

    @Test
    fun `above the limit is exceeded and the remaining is negative`() {
        val progress = BudgetProgress(limit = 100.0, spent = 130.0)

        assertEquals(BudgetLevel.EXCEEDED, progress.level)
        assertEquals(1.3, progress.ratio!!, 1e-9)
        assertEquals(-30.0, progress.remaining!!, 1e-9)
    }

    @Test
    fun `nothing spent is ok`() {
        assertEquals(BudgetLevel.OK, BudgetProgress(limit = 100.0, spent = 0.0).level)
    }
}
