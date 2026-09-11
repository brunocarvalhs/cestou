package br.com.brunocarvalhs.howmuch.feature.auth.presentation.components

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * A [Shape] whose top edge is a single asymmetric wave (a wide "belly" on the left, rising to a
 * crest around the middle, then settling into a smaller dip on the right) instead of a straight
 * line. Meant to clip a content sheet so it reads as resting on top of a colored hero area above
 * it, e.g. [br.com.brunocarvalhs.howmuch.feature.auth.presentation.screen.WelcomeScreen].
 *
 * @param waveHeight how tall the wave motion is, i.e. how far the top edge travels between its
 * lowest and highest points.
 */
class WaveTopShape(private val waveHeight: Dp) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val amplitude = with(density) { waveHeight.toPx() }
        val path = Path().apply {
            moveTo(0f, amplitude * 1.6f)
            cubicTo(
                size.width * 0.22f, amplitude * 2.4f,
                size.width * 0.38f, 0f,
                size.width * 0.62f, amplitude * 0.35f
            )
            cubicTo(
                size.width * 0.82f, amplitude * 0.65f,
                size.width * 0.92f, amplitude * 1.9f,
                size.width, amplitude * 1.15f
            )
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}
