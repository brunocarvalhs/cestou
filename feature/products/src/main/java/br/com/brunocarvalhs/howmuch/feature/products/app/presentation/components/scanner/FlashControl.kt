package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun FlashControl(
    isFlashOn: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        modifier = modifier.padding(24.dp),
        onClick = onToggle,
        containerColor = if (isFlashOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Icon(
            imageVector = if (isFlashOn) Icons.Default.FlashlightOff else Icons.Default.FlashlightOn,
            contentDescription = "Flash"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FlashControlOnPreview() {
    FlashControl(isFlashOn = true, onToggle = {})
}

@Preview(showBackground = true)
@Composable
private fun FlashControlOffPreview() {
    FlashControl(isFlashOn = false, onToggle = {})
}
