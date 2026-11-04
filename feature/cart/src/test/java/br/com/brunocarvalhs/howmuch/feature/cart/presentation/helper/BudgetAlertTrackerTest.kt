package br.com.brunocarvalhs.howmuch.feature.cart.presentation.helper

import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetAlert
import br.com.brunocarvalhs.howmuch.feature.cart.presentation.state.BudgetScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BudgetAlertTrackerTest {

    private val tracker = BudgetAlertTracker()

    private fun list(spent: Double) = BudgetProgress(limit = 100.0, spent = spent)

    @Test
    fun `nothing is reported while the data is still loading`() {
        assertNull(tracker.onUpdate(ready = false, list = list(95.0), month = null))
    }

    @Test
    fun `opening a list that is already over the line does not warn`() {
        assertNull(tracker.onUpdate(ready = true, list = list(95.0), month = null))
    }

    @Test
    fun `crossing 80 percent warns once`() {
        tracker.onUpdate(ready = true, list = list(50.0), month = null)

        val alert = tracker.onUpdate(ready = true, list = list(85.0), month = null)

        assertEquals(BudgetAlert(BudgetScope.LIST, BudgetLevel.WARNING), alert)
        assertNull(tracker.onUpdate(ready = true, list = list(90.0), month = null))
    }

    @Test
    fun `crossing 100 percent after the warning alerts once more`() {
        tracker.onUpdate(ready = true, list = list(50.0), month = null)
        tracker.onUpdate(ready = true, list = list(85.0), month = null)

        val alert = tracker.onUpdate(ready = true, list = list(120.0), month = null)

        assertEquals(BudgetAlert(BudgetScope.LIST, BudgetLevel.EXCEEDED), alert)
        assertNull(tracker.onUpdate(ready = true, list = list(150.0), month = null))
    }

    @Test
    fun `going back under and over again does not repeat the alert`() {
        tracker.onUpdate(ready = true, list = list(50.0), month = null)
        tracker.onUpdate(ready = true, list = list(85.0), month = null)
        tracker.onUpdate(ready = true, list = list(60.0), month = null)

        assertNull(tracker.onUpdate(ready = true, list = list(85.0), month = null))
    }

    @Test
    fun `a list that jumps straight past 100 percent alerts as exceeded, not as a warning`() {
        tracker.onUpdate(ready = true, list = list(10.0), month = null)

        val alert = tracker.onUpdate(ready = true, list = list(130.0), month = null)

        assertEquals(BudgetLevel.EXCEEDED, alert?.level)
    }

    @Test
    fun `the month scope alerts on its own`() {
        val month = { spent: Double -> BudgetProgress(limit = 1000.0, spent = spent) }
        tracker.onUpdate(ready = true, list = null, month = month(100.0))

        val alert = tracker.onUpdate(ready = true, list = null, month = month(900.0))

        assertEquals(BudgetAlert(BudgetScope.MONTH, BudgetLevel.WARNING), alert)
    }

    @Test
    fun `when both scopes cross together the exceeded one wins, then the list`() {
        tracker.onUpdate(ready = true, list = list(10.0), month = BudgetProgress(1000.0, 10.0))

        val exceededMonth = tracker.onUpdate(
            ready = true,
            list = list(85.0),
            month = BudgetProgress(1000.0, 1100.0)
        )
        assertEquals(BudgetAlert(BudgetScope.MONTH, BudgetLevel.EXCEEDED), exceededMonth)

        tracker.onUpdate(ready = true, list = list(10.0), month = BudgetProgress(1000.0, 10.0))
    }

    @Test
    fun `lists and months without a limit never alert`() {
        tracker.onUpdate(ready = true, list = null, month = null)

        assertNull(tracker.onUpdate(ready = true, list = null, month = null))
    }
}
