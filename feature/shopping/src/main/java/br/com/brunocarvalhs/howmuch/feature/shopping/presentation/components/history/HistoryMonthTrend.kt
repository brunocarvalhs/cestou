package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

private const val LINE_PADDING_FRACTION = 0.15f
private const val HALF = 0.5f

/** Each total as a 0..1 fraction of the largest one; every ratio is 0 when nothing was spent. */
internal fun trendRatios(totals: List<Double>): List<Float> {
    val max = totals.maxOrNull()?.takeIf { it > 0.0 } ?: return totals.map { 0f }
    return totals.map { (it / max).toFloat() }
}

/**
 * One card's slice of the trend line: the point for its own month plus half-segments towards the
 * neighbours (their midpoint with this month), so the slices of adjacent cards join into a single
 * polyline across the carousel. A lone month draws only the point.
 */
@Composable
internal fun HistoryMonthTrend(
    ratio: Float,
    previousRatio: Float?,
    nextRatio: Float?,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().height(40.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(40.dp).clearAndSetSemantics { }) {
            fun y(value: Float) = size.height * (1f - LINE_PADDING_FRACTION) -
                value * size.height * (1f - 2 * LINE_PADDING_FRACTION)

            val stroke = 2.dp.toPx()
            val point = Offset(size.width * HALF, y(ratio))
            previousRatio?.let {
                drawLine(color, Offset(0f, y((it + ratio) * HALF)), point, stroke, StrokeCap.Round)
            }
            nextRatio?.let {
                drawLine(color, point, Offset(size.width, y((it + ratio) * HALF)), stroke, StrokeCap.Round)
            }
            drawCircle(color, radius = stroke * 2, center = point)
        }
    }
}
