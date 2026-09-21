package br.com.brunocarvalhs.howmuch.feature.settings.presentation.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class LegalContentScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking the view online button opens the hosted legal url`() {
        var openedUrl: String? = null
        val hostedUrl = "https://bruno-carvalho.dev.br/legal?doc=cestou-privacy-policy"

        composeTestRule.setContent {
            CestouTheme {
                LegalContentScreen(
                    title = "Política de Privacidade",
                    content = "Conteúdo local de exemplo.",
                    url = hostedUrl,
                    onOpenUrl = { openedUrl = it },
                    onBack = {}
                )
            }
        }

        composeTestRule.onNodeWithText("View full version online").performClick()

        assertEquals(hostedUrl, openedUrl)
    }
}
