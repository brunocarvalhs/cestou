package br.com.brunocarvalhs.howmuch.core.theme

import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview

/**
 * Multipreview para telas de celular e tablet, em orientação vertical (retrato)
 * e horizontal (paisagem), no modo claro e escuro.
 *
 * Tamanhos (dp), alinhados às faixas de largura do Material3:
 *  - Pequeno:   320 x 640   (celulares compactos antigos)
 *  - Compacto:  360 x 780   (faixa compact)
 *  - Médio:     411 x 891   (faixa medium)
 *  - Expandido: 800 x 1280  (faixa expanded / tablets)
 *
 * Na horizontal a largura e a altura são invertidas.
 */
@Preview(name = "Pequeno", group = "Vertical", showBackground = true,
    device = "spec:width=320dp,height=640dp,dpi=320")
@Preview(name = "Compacto", group = "Vertical", showBackground = true,
    device = "spec:width=360dp,height=780dp,dpi=420")
@Preview(name = "Médio", group = "Vertical", showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420")
@Preview(name = "Expandido", group = "Vertical", showBackground = true,
    device = "spec:width=800dp,height=1280dp,dpi=240")

@Preview(name = "Pequeno - Escuro", group = "Vertical", showBackground = true,
    device = "spec:width=320dp,height=640dp,dpi=320",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Compacto - Escuro", group = "Vertical", showBackground = true,
    device = "spec:width=360dp,height=780dp,dpi=420",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Médio - Escuro", group = "Vertical", showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Expandido - Escuro", group = "Vertical", showBackground = true,
    device = "spec:width=800dp,height=1280dp,dpi=240",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)

@Preview(name = "Pequeno", group = "Horizontal", showBackground = true,
    device = "spec:width=640dp,height=320dp,dpi=320")
@Preview(name = "Compacto", group = "Horizontal", showBackground = true,
    device = "spec:width=780dp,height=360dp,dpi=420")
@Preview(name = "Médio", group = "Horizontal", showBackground = true,
    device = "spec:width=891dp,height=411dp,dpi=420")
@Preview(name = "Expandido", group = "Horizontal", showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240")

@Preview(name = "Pequeno - Escuro", group = "Horizontal", showBackground = true,
    device = "spec:width=640dp,height=320dp,dpi=320",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Compacto - Escuro", group = "Horizontal", showBackground = true,
    device = "spec:width=780dp,height=360dp,dpi=420",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Médio - Escuro", group = "Horizontal", showBackground = true,
    device = "spec:width=891dp,height=411dp,dpi=420",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Expandido - Escuro", group = "Horizontal", showBackground = true,
    device = "spec:width=1280dp,height=800dp,dpi=240",
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
annotation class PreviewCestouScreens