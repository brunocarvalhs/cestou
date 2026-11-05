package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.viewmodel

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.analytics.contract.AnalyticsTracker
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsEvents
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsParams
import br.com.brunocarvalhs.howmuch.core.analytics.model.AnalyticsUserProperties
import br.com.brunocarvalhs.howmuch.core.domain.model.FeedbackPrompt
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.feature.feedback.data.review.PlayReviewLauncher
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.repository.FeedbackStateStore
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.DismissSurveyUseCase
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.SubmitCsatUseCase
import br.com.brunocarvalhs.howmuch.feature.feedback.domain.usecase.SubmitNpsUseCase
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FeedbackSurveyViewModelTest {

    private val store = mockk<FeedbackStateStore>(relaxed = true)
    private val analytics = mockk<AnalyticsTracker>(relaxed = true)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun viewModel() = FeedbackSurveyViewModel(
        context,
        SubmitNpsUseCase(store),
        SubmitCsatUseCase(store),
        DismissSurveyUseCase(store),
        mockk<PlayReviewLauncher>(relaxed = true),
        analytics
    )

    @Test
    fun `nps scores map to the right class and set the user property`() {
        mapOf(
            0 to "detractor",
            6 to "detractor",
            7 to "passive",
            8 to "passive",
            9 to "promoter",
            10 to "promoter"
        ).forEach { (score, expected) ->
            val vm = viewModel().apply { setPrompt("NPS", null) }

            vm.intent.onScoreSelected(score)

            verify {
                analytics.trackEvent(
                    AnalyticsEvents.NPS_SUBMITTED,
                    mapOf(AnalyticsParams.SCORE to score, AnalyticsParams.VALUE to expected)
                )
            }
            verify { analytics.setUserProperty(AnalyticsUserProperties.NPS_CLASS, expected) }
            verify { store.recordAnswered(FeedbackPrompt(SurveyKind.NPS), score) }
        }
    }

    @Test
    fun `csat scores are reported with the feature`() {
        (1..5).forEach { score ->
            val vm = viewModel().apply { setPrompt("CSAT", "purchase") }

            vm.intent.onScoreSelected(score)

            verify {
                analytics.trackEvent(
                    AnalyticsEvents.CSAT_SUBMITTED,
                    mapOf(AnalyticsParams.FEATURE to "purchase", AnalyticsParams.SCORE to score)
                )
            }
        }
    }

    @Test
    fun `a low score offers the contact and a high one does not`() {
        val low = viewModel().apply { setPrompt("CSAT", "ai_chat") }
        low.intent.onScoreSelected(2)
        val high = viewModel().apply { setPrompt("NPS", null) }
        high.intent.onScoreSelected(9)

        assertTrue(low.uiState.value.offerContact)
        assertTrue(low.uiState.value.submitted)
        assertFalse(high.uiState.value.offerContact)
    }

    @Test
    fun `only the first score counts`() {
        val vm = viewModel().apply { setPrompt("CSAT", "ai_chat") }

        vm.intent.onScoreSelected(5)
        vm.intent.onScoreSelected(1)

        verify(exactly = 1) { analytics.trackEvent(AnalyticsEvents.CSAT_SUBMITTED, any()) }
        assertEquals(5, vm.uiState.value.selectedScore)
    }

    @Test
    fun `dismissing without an answer applies the backoff and reports it`() {
        val vm = viewModel().apply { setPrompt("CSAT", "ai_chat") }

        vm.intent.onDismiss()

        verify { store.recordDismissed(FeedbackPrompt(SurveyKind.CSAT, "ai_chat")) }
        verify {
            analytics.trackEvent(
                AnalyticsEvents.FEEDBACK_PROMPT_DISMISSED,
                mapOf(AnalyticsParams.KIND to "csat", AnalyticsParams.FEATURE to "ai_chat")
            )
        }
    }

    @Test
    fun `closing after answering is not a dismissal`() {
        val vm = viewModel().apply { setPrompt("NPS", null) }
        vm.intent.onScoreSelected(10)

        vm.intent.onDismiss()

        verify(exactly = 0) { store.recordDismissed(any()) }
    }

    @Test
    fun `contact support reports the event`() {
        val vm = viewModel().apply { setPrompt("NPS", null) }

        vm.intent.onContactSupport()

        verify {
            analytics.trackEvent(
                AnalyticsEvents.FEEDBACK_CONTACT_OPENED,
                mapOf(AnalyticsParams.KIND to "nps", AnalyticsParams.FEATURE to "")
            )
        }
    }

    @Test
    fun `an unknown kind leaves the survey empty`() {
        val vm = viewModel().apply { setPrompt("bogus", null) }

        assertEquals(null, vm.uiState.value.kind)
        vm.intent.onScoreSelected(5)
        verify(exactly = 0) { analytics.trackEvent(any(), any()) }
    }

    @Test
    fun `no event parameter carries free text`() {
        val captured = mutableListOf<Map<String, Any?>>()
        val params = slot<Map<String, Any?>>()
        io.mockk.every { analytics.trackEvent(any(), capture(params)) } answers { captured += params.captured }
        val vm = viewModel().apply { setPrompt("CSAT", "ai_chat") }

        vm.intent.onScoreSelected(1)
        vm.intent.onContactSupport()

        captured.flatMap { it.values }.forEach { assertTrue(it is String || it is Int || it is Boolean) }
        assertTrue(captured.isNotEmpty())
    }
}
