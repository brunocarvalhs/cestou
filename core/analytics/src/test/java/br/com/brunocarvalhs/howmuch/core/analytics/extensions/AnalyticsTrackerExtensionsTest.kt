package br.com.brunocarvalhs.howmuch.core.analytics.extensions

import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.io.IOException

class AnalyticsTrackerExtensionsTest {

    private val tracker = mockk<AnalyticsTracker>(relaxed = true)

    @Test
    fun `trackAction sends ui_action with the element id`() {
        tracker.trackAction("cart.share", mapOf(AnalyticsParams.SHOPPING_ID to "l1"))

        verify {
            tracker.trackEvent(
                AnalyticsEvents.UI_ACTION,
                mapOf(AnalyticsParams.SHOPPING_ID to "l1", AnalyticsParams.ELEMENT_ID to "cart.share")
            )
        }
    }

    @Test
    fun `trackError sends only the exception class never its message`() {
        tracker.trackError("save_product", IOException("secret@mail.com"))

        verify {
            tracker.trackEvent(
                AnalyticsEvents.OPERATION_FAILED,
                mapOf(AnalyticsParams.OPERATION to "save_product", AnalyticsParams.ERROR_TYPE to "IOException")
            )
        }
    }
}
