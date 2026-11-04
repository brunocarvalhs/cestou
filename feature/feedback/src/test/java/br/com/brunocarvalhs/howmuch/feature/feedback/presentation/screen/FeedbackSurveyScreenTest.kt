package br.com.brunocarvalhs.howmuch.feature.feedback.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.domain.model.SurveyKind
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.intent.FeedbackSurveyIntent
import br.com.brunocarvalhs.howmuch.feature.feedback.presentation.state.FeedbackSurveyUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class FeedbackSurveyScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `tapping an emoji reports its score`() {
        var score: Int? = null
        composeTestRule.setContent {
            CestouTheme {
                FeedbackSurveyScreen(
                    state = FeedbackSurveyUiState(kind = SurveyKind.CSAT, feature = "purchase"),
                    intent = FeedbackSurveyIntent(onScoreSelected = { score = it })
                )
            }
        }

        composeTestRule.onNodeWithTag("feedback_score_4").performClick()

        assertEquals(4, score)
    }

    @Test
    fun `nps shows eleven scores and dismiss`() {
        var dismissed = false
        composeTestRule.setContent {
            CestouTheme {
                FeedbackSurveyScreen(
                    state = FeedbackSurveyUiState(kind = SurveyKind.NPS),
                    intent = FeedbackSurveyIntent(onDismiss = { dismissed = true })
                )
            }
        }

        (0..10).forEach { composeTestRule.onNodeWithTag("feedback_score_$it").assertExists() }
        composeTestRule.onNodeWithTag("feedback_dismiss").performClick()

        assertEquals(true, dismissed)
    }

    @Test
    fun `a low answer offers the contact`() {
        var contacted = false
        composeTestRule.setContent {
            CestouTheme {
                FeedbackSurveyScreen(
                    state = FeedbackSurveyUiState(
                        kind = SurveyKind.CSAT,
                        feature = "ai_chat",
                        selectedScore = 1,
                        submitted = true,
                        offerContact = true
                    ),
                    intent = FeedbackSurveyIntent(onContactSupport = { contacted = true })
                )
            }
        }

        composeTestRule.onNodeWithTag("feedback_contact").performClick()
        composeTestRule.onNodeWithTag("feedback_done").assertExists()

        assertEquals(true, contacted)
    }
}
