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
private const val GUIDE_WIDTH_FRACTION = 0.85f
private const val GUIDE_HEIGHT_FRACTION = 0.32f
private const val CORNER_RADIUS = 24f
private const val STROKE_WIDTH = 4f

/**
 * Framing guide shown over the live camera preview to help the user position a
 * (wide, rectangular) price tag in frame, mirroring the square guide the barcode
 * scanner used to have.
 */
@Composable
internal fun PriceTagScannerCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color.Black.copy(alpha = BACKGROUND_ALPHA))

        val guideWidth = size.width * GUIDE_WIDTH_FRACTION
        val guideHeight = guideWidth * GUIDE_HEIGHT_FRACTION

        val left = (size.width - guideWidth) / 2
        val top = (size.height - guideHeight) / 2

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(guideWidth, guideHeight),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(guideWidth, guideHeight),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            style = Stroke(width = STROKE_WIDTH)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PriceTagScannerCanvasPreview() {
    PriceTagScannerCanvas()
}
