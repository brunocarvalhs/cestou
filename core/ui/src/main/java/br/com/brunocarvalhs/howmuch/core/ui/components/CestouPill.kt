package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalCoral
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalMist
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNavy
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNight

@Composable
fun CestouPill(text: String, tone: CestouPillTone, modifier: Modifier = Modifier) {
    val (container, content) = when (tone) {
        CestouPillTone.OnPrimary -> Color.White.copy(alpha = 0.18f) to Color.White
        CestouPillTone.Neutral -> SubtotalMist to SubtotalNavy
        CestouPillTone.Accent -> SubtotalCoral to SubtotalNight
    }
    Text(
        text = text,
        modifier = modifier
            .background(container, RoundedCornerShape(percent = 50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        color = content
    )
}

@Preview
@Composable
private fun CestouPillPreview() {
    CestouPill(text = "Perto do limite", tone = CestouPillTone.Accent)
}
