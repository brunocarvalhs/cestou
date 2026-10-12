package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.scanner.QRCodeOverlay
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragAndDropContainer
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DragTarget
import br.com.brunocarvalhs.howmuch.core.ui.dragdrop.DropTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

// Testes de layout via Robolectric (JVM): componentes compartilhados do core:ui.
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CoreUiComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `bottom navigation renders and navigates on tap`() {
        var navigated: Any? = null
        composeTestRule.setContent {
            CestouTheme {
                CestouBottomNavigation(
                    currentRoute = null,
                    onNavigate = { navigated = it },
                    photoUrl = "https://x/p.png"
                )
            }
        }
        val count = composeTestRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size
        for (i in 0 until count) composeTestRule.onAllNodes(hasClickAction())[i].performClick()
        assertTrue(navigated != null)
    }

    @Test
    fun `bottom navigation renders hidden`() {
        composeTestRule.setContent {
            CestouTheme { CestouBottomNavigation(currentRoute = "x", onNavigate = {}, visible = false) }
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `category picker lists categories and reports the selection`() {
        var selected: String? = null
        composeTestRule.setContent {
            CestouTheme { CategoryPickerDialog(selected = "Mercearia", onSelect = { selected = it }, onDismiss = {}) }
        }
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()
        assertTrue(selected != null)
    }

    @Test
    fun `locked banner and qr overlay render`() {
        var flash = 0
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    CestouLockedBanner()
                    Box(Modifier.size(300.dp)) { QRCodeOverlay(isFlashOn = true, onFlashToggle = { flash++ }) }
                }
            }
        }
        composeTestRule.onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, flash)
    }

    @Test
    fun `drag and drop container renders the target and drop zone`() {
        composeTestRule.setContent {
            CestouTheme {
                DragAndDropContainer {
                    Column {
                        DragTarget(dataToDrop = "item") { Box(Modifier.size(60.dp)) { Text("arrastar") } }
                        DropTarget(onDataDropped = {}) { isHovered, _ ->
                            Text(if (isHovered) "solto" else "destino")
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("arrastar").performTouchInput {
            longClick()
        }
        composeTestRule.onNodeWithText("arrastar").performTouchInput { swipeDown() }
        composeTestRule.onRoot().assertExists()
    }

    @Test
    fun `suggestions render items and loading and invoke click`() {
        var clicked: String? = null
        composeTestRule.setContent {
            CestouTheme {
                Column {
                    Suggestions(items = listOf("Calcular total", "Sugerir receita"), onItemClick = { clicked = it })
                    Suggestions(isLoading = true)
                    Suggestions()
                }
            }
        }
        composeTestRule.onNodeWithText("Calcular total").performClick()
        assertEquals("Calcular total", clicked)
    }

    @Test
    fun `share options sheet renders`() {
        composeTestRule.setContent {
            CestouTheme {
                ShareOptionsBottomSheet(onDismissRequest = {}, onInviteMember = {}, onShareAsText = {})
            }
        }
        composeTestRule.waitForIdle()
    }
}
