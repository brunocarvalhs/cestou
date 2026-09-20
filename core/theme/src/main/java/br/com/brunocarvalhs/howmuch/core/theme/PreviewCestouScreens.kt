package br.com.brunocarvalhs.howmuch.core.theme

import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview for mobile (phone) screens: renders each annotated `@Preview`
 * function at three breakpoints aligned with Material3 window-size-class
 * widths (~360dp compact, ~411dp medium, ~800dp expanded).
 */
@Preview(
    name = "Compacto",
    group = "Responsivo",
    showBackground = true,
    device = "spec:width=360dp,height=780dp,dpi=420"
)
@Preview(
    name = "Médio",
    group = "Responsivo",
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420"
)
@Preview(
    name = "Expandido",
    group = "Responsivo",
    showBackground = true,
    device = "spec:width=800dp,height=1280dp,dpi=240"
)
annotation class PreviewCestouScreens
