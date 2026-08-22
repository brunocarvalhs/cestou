package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner

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
private const val WIDTH_FRACTION = 0.82f
private const val HEIGHT_FRACTION = 0.55f
private const val CORNER_RADIUS = 32f
private const val STROKE_WIDTH = 4f

@Composable
internal fun StaticScannerCanvas() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color.Black.copy(alpha = BACKGROUND_ALPHA))

        val width = size.width * WIDTH_FRACTION
        val height = width * HEIGHT_FRACTION
        val left = (size.width - width) / 2
        val top = (size.height - height) / 2

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(width, height),
            cornerRadius = CornerRadius(CORNER_RADIUS),
            style = Stroke(width = STROKE_WIDTH)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StaticScannerCanvasPreview() {
    StaticScannerCanvas()
}
