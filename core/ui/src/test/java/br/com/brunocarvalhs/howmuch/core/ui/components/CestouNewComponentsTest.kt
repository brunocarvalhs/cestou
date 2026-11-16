package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CestouNewComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `segmented emits the option only when it changes`() {
        val selections = mutableListOf<String>()
        composeTestRule.setContent {
            CestouTheme {
                CestouSegmented(
                    options = listOf("Semana", "Mês"),
                    selected = "Mês",
                    label = { it },
                    onSelect = { selections.add(it) }
                )
            }
        }

        composeTestRule.onNodeWithText("Mês").performClick()
        assertTrue("tocar na opção já selecionada não deve emitir", selections.isEmpty())

        composeTestRule.onNodeWithText("Semana").performClick()
        assertEquals(listOf("Semana"), selections)
    }

    @Test
    fun `check row reflects checked state and toggles on tap`() {
        var checked by mutableStateOf(false)
        composeTestRule.setContent {
            CestouTheme {
                CestouCheckRow(
                    title = "Arroz integral 5 kg",
                    price = "R$ 27,90",
                    checked = checked,
                    onCheckedChange = { checked = it },
                    modifier = Modifier.testTag("check_row")
                )
            }
        }

        composeTestRule.onNodeWithTag("check_row").assertIsOff()
        composeTestRule.onNodeWithTag("check_row").performClick()
        composeTestRule.waitForIdle()
        assertTrue(checked)
        composeTestRule.onNodeWithTag("check_row").assertIsOn()
    }

    @Test
    fun `pill and section label render their text`() {
        composeTestRule.setContent {
            CestouTheme {
                CestouPill(text = "Finalizada", tone = CestouPillTone.Neutral)
                CestouSectionLabel(text = "Hortifruti")
            }
        }

        composeTestRule.onNodeWithText("Finalizada").assertIsDisplayed()
        composeTestRule.onNodeWithText("HORTIFRUTI").assertIsDisplayed()
    }

    @Test
    fun `button renders every style with its label`() {
        composeTestRule.setContent {
            CestouTheme {
                CestouButtonStyle.entries.forEach { style ->
                    CestouButton(text = style.name, onClick = {}, style = style)
                }
            }
        }

        CestouButtonStyle.entries.forEach { style ->
            composeTestRule.onNodeWithText(style.name).assertIsDisplayed()
        }
    }
}
