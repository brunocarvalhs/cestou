package br.com.brunocarvalhs.howmuch.feature.chat.presentation.helper

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalMaterial3Api::class)
class ChatSheetPolicyTest {

    @Test
    fun `an active chat allows Expanded and Hidden so back and scrim can close it`() {
        assertTrue(isSheetValueAllowed(isChatActive = true, target = SheetValue.Expanded))
        assertTrue(isSheetValueAllowed(isChatActive = true, target = SheetValue.Hidden))
    }

    @Test
    fun `an active chat still rejects PartiallyExpanded`() {
        assertFalse(isSheetValueAllowed(isChatActive = true, target = SheetValue.PartiallyExpanded))
    }

    @Test
    fun `an idle chat allows every value`() {
        SheetValue.entries.forEach { assertTrue(isSheetValueAllowed(isChatActive = false, target = it)) }
    }
}
