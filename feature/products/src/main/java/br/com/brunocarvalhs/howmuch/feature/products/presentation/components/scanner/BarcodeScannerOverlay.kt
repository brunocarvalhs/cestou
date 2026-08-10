package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.feature.products.R

@Composable
fun BarcodeScannerOverlay(
    isFlashOn: Boolean,
    onFlashToggle: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        StaticScannerCanvas()

        Text(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 96.dp),
            text = stringResource(R.string.product_barcode_instruction),
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )

        FlashControl(
            isFlashOn = isFlashOn,
            onToggle = onFlashToggle,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BarcodeScannerOverlayPreview() {
    BarcodeScannerOverlay(isFlashOn = false, onFlashToggle = {})
}
