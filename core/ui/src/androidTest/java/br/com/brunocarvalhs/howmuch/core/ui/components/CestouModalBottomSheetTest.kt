package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * sdd-035: o sheet usa `colorScheme.background` e o conteúdo com `Scaffold` transparente herda esse
 * fundo; o teste cobre os dois temas. A cor do handle em si é conferida manualmente (Maestro).
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class CestouModalBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun showSheet(darkTheme: Boolean) {
        composeTestRule.setContent {
            CestouTheme(darkTheme = darkTheme) {
                CestouModalBottomSheet(onDismissRequest = {}) {
                    Scaffold(containerColor = Color.Transparent) { padding ->
                        Box(modifier = Modifier.testTag(CONTENT_TAG))
                        padding
                    }
                }
            }
        }
    }

    @Test
    fun showsItsContentInTheLightTheme() {
        showSheet(darkTheme = false)

        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    @Test
    fun showsItsContentInTheDarkTheme() {
        showSheet(darkTheme = true)

        composeTestRule.onNodeWithTag(CONTENT_TAG).assertIsDisplayed()
    }

    private companion object {
        const val CONTENT_TAG = "sheet_content"
    }
}
