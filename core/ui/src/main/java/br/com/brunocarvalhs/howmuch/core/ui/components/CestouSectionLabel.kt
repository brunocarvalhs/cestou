package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Rótulo de grupo (ex.: "HORTIFRUTI", "HOJE"): 12 sp, caixa alta, tracking 0.06em, cinza de apoio.
 */
@Composable
fun CestouSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 12.sp,
            letterSpacing = 0.06.em
        ),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Preview
@Composable
private fun CestouSectionLabelPreview() {
    CestouSectionLabel(text = "Hortifruti")
}
