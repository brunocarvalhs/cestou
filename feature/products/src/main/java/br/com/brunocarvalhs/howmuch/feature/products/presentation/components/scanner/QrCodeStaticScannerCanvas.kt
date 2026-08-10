package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.scanner

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview

private const val BACKGROUND_ALPHA = 0.55f
private const val SCANNER_SIZE_FRACTION = 0.75f
private const val CORNER_RADIUS = 32f
private const val STROKE_WIDTH = 4f

@Composable
internal fun QrCodeStaticScannerCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Fundo escurecido
        drawRect(Color.Black.copy(alpha = BACKGROUND_ALPHA))

        // Tamanho do quadrado
        val scannerSize = size.width * SCANNER_SIZE_FRACTION

        val left = (size.width - scannerSize) / 2
        val top = (size.height - scannerSize) / 2

        // "Recorte" transparente
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            blendMode = BlendMode.Clear
        )

        // Borda branca
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scannerSize, scannerSize),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            style = Stroke(width = STROKE_WIDTH)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun QrCodeStaticScannerCanvasPreview() {
    QrCodeStaticScannerCanvas()
}
