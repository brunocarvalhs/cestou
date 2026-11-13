package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text as PhoneText
import androidx.wear.compose.material3.Text as WearText

/**
 * The pairing code as two groups of four letters, the way both devices show it (sdd-055).
 * The whole code is also the content description, so a screen reader reads it as one value.
 *
 * Two variants instead of one generic composable: the watch and the phone use different `Text`
 * components (Wear Material 3 and Material 3).
 */
@Composable
internal fun WearPairingCode(code: String, tag: String, style: TextStyle, color: Color) {
    val (first, second) = halves(code)
    Column(
        modifier = Modifier.testTag(tag).semantics { contentDescription = code },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        WearText(text = first, style = style, color = color, fontWeight = FontWeight.Bold, fontSize = CODE_FONT_SIZE)
        WearText(text = second, style = style, color = color, fontWeight = FontWeight.Bold, fontSize = CODE_FONT_SIZE)
    }
}

@Composable
internal fun PhonePairingCode(code: String, tag: String, style: TextStyle, color: Color) {
    val (first, second) = halves(code)
    Column(
        modifier = Modifier.testTag(tag).semantics { contentDescription = code },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PhoneText(text = first, style = style, color = color, fontWeight = FontWeight.Bold)
        PhoneText(text = second, style = style, color = color, fontWeight = FontWeight.Bold)
    }
}

/** `"BDFG HKLM"` becomes `"BDFG"` and `"HKLM"`. A code without a space is shown on one line. */
private fun halves(code: String): Pair<String, String> {
    val parts = code.split(' ')
    return parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
}

private val CODE_FONT_SIZE = 26.sp
