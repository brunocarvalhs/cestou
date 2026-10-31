package br.com.brunocarvalhs.howmuch.feature.chat.presentation.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

private const val COLOR_PEACH = 0xFFFFB74D
private const val COLOR_ORANGE = 0xFFFF9800
private const val COLOR_ORANGE_DARK = 0xFFF57C00
private const val COLOR_ORANGE_DEEP = 0xFFE65100
private const val COLOR_ORANGE_RED = 0xFFFF7043
private const val ANIMATION_DURATION_MS = 12000
private const val GRADIENT_OFFSET_TARGET = 1000f
private const val GRADIENT_OFFSET_SPAN = 1000f

@Composable
fun AnimatedColorText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    autoSize: TextAutoSize? = null,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "GradientTransition")

    val offsetAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = GRADIENT_OFFSET_TARGET,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "GradientOffset",
    )

    val gradientColors = listOf(
        Color(COLOR_PEACH),
        Color(COLOR_ORANGE),
        Color(COLOR_ORANGE_DARK),
        Color(COLOR_ORANGE_DEEP),
        Color(COLOR_ORANGE_RED),
        Color(COLOR_PEACH),
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val animatedBrush = Brush.linearGradient(
            colors = gradientColors,
            start = Offset(offsetAnim, offsetAnim),
            end = Offset(offsetAnim + GRADIENT_OFFSET_SPAN, offsetAnim + GRADIENT_OFFSET_SPAN),
        )

        Text(
            modifier = modifier,
            text = text,
            color = color,
            autoSize = autoSize,
            fontSize = fontSize,
            fontStyle = fontStyle,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textDecoration = textDecoration,
            textAlign = textAlign,
            lineHeight = lineHeight,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            onTextLayout = onTextLayout,
            style = TextStyle(
                brush = animatedBrush,
                fontSize = fontSize,
                fontWeight = fontWeight,
            ),
        )
    }
}
