package br.com.brunocarvalhs.howmuch.core.theme

import androidx.compose.ui.graphics.Color

/**
 * Subtotal brand palette (nova identidade).
 * Named by role so the source of truth reads correctly.
 */
val SubtotalBlue = Color(0xFF2F55D4)
val SubtotalNavy = Color(0xFF1E3FA8)
val SubtotalNight = Color(0xFF0B1A3F)
val SubtotalCoral = Color(0xFFFF7A59)
val SubtotalCoralDeep = Color(0xFFB4421F)
val SubtotalCoralSoft = Color(0xFFFFF1EB)
val SubtotalMist = Color(0xFFEEF3FF)
val SubtotalSlate = Color(0xFF5B6478)
val SubtotalInk = Color(0xFF1B2A4A)
val SubtotalMuted = Color(0xFF9AA4B8)
val SubtotalFog = Color(0xFFF7F9FD)
val SubtotalLine = Color(0xFFDDE3F0)

/**
 * Dark mode surfaces, derived from [SubtotalNight].
 */
val SubtotalDarkBackground = Color(0xFF0B1226)
val SubtotalDarkSurface = Color(0xFF15203D)
val SubtotalDarkSurfaceVariant = Color(0xFF1C2846)
val SubtotalDarkLine = Color(0xFF2A3A63)
val SubtotalDarkBlue = Color(0xFF8FA9FF)
val SubtotalDarkMuted = Color(0xFFA9B8E6)

/**
 * Cestou names kept as aliases so existing call sites keep compiling.
 * Each alias now points at its role in the Subtotal palette.
 */
val CestouDarkGreen = SubtotalNight
val CestouBrightGreen = SubtotalBlue
val CestouSoftGreen = SubtotalMist
val CestouDeepGreen = SubtotalNavy

val CestouOrange = SubtotalCoral
val CestouSoftOrange = SubtotalCoralSoft

val CestouTextPrimary = SubtotalInk
val CestouTextSecondary = SubtotalSlate
val CestouTextTertiary = SubtotalMuted

val SurfaceLight = Color(0xFFFFFFFF)
val BackgroundLight = SubtotalFog
val BorderLight = SubtotalLine

val SurfaceDark = SubtotalDarkSurface
val BackgroundDark = SubtotalDarkBackground
val BorderDark = SubtotalDarkLine

/**
 * Semantic Colors
 */
val Success = Color(0xFF22C55E)
val Error = SubtotalCoralDeep
val Warning = Color(0xFFF59E0B)
val Info = SubtotalBlue

// Legacy compatibility (to be removed after full migration)
val CestouGreenPrimary = CestouBrightGreen
val CestouGreenSecondary = CestouSoftGreen
val CestouOrangePrimary = CestouOrange
val CestouOrangeSecondary = CestouSoftOrange
val CestouGreenDarkPrimary = CestouBrightGreen
val CestouGreenDarkSecondary = CestouDeepGreen
val CestouOrangeDarkPrimary = CestouOrange
val CestouOrangeDarkSecondary = SubtotalCoralDeep
