package br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Layout test (Robolectric, JVM). Selectors are testTags, not text (AGENTS §5).
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LinkWearDeviceScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `the profile entry explains the watch pairing`() {
        composeTestRule.setContent {
            LinkWearDeviceScreen(onBack = {})
        }

        composeTestRule.onNodeWithTag("link_wear_instructions").assertIsDisplayed()
    }
}
