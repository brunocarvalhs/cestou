package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val SIZE = 24f
private const val OUTLINE_STROKE = 2f
private const val BAR_WIDTH = 2f
private const val BAR_BOTTOM = 20f

/** Left edge and top of each of the three bars. */
private val BARS = listOf(Bar(4f, 12f), Bar(10f, 6f), Bar(16f, 3f))

private data class Bar(val left: Float, val top: Float)

/** Three bars of rising height, drawn in-house so the tab icon needs no extra icon dependency. */
private fun bars(name: String, filled: Boolean): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = SIZE.dp,
    defaultHeight = SIZE.dp,
    viewportWidth = SIZE,
    viewportHeight = SIZE
).apply {
    BARS.forEach { (x, top) ->
        path(
            fill = if (filled) SolidColor(Color.Black) else null,
            stroke = if (filled) null else SolidColor(Color.Black),
            strokeLineWidth = OUTLINE_STROKE,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(x, top)
            horizontalLineTo(x + BAR_WIDTH)
            verticalLineTo(BAR_BOTTOM)
            horizontalLineTo(x)
            close()
        }
    }
}.build()

object HistoryBarsIcon {
    val Filled: ImageVector by lazy { bars("HistoryBarsFilled", filled = true) }
    val Outlined: ImageVector by lazy { bars("HistoryBarsOutlined", filled = false) }
}
