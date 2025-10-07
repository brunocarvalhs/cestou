package br.com.brunocarvalhs.howmuch.app.foundation.annotations

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

@Preview(
    name = "Phone",
    device = "spec:width=360dp,height=640dp,dpi=480",
    showBackground = true
)
@Preview(
    name = "Phone - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=360dp,height=640dp,dpi=480",
    showBackground = true
)
@Preview(
    name = "Foldable",
    device = "spec:width=673dp,height=841dp,dpi=480",
    showBackground = true
)
@Preview(
    name = "Tablet",
    device = "spec:width=1280dp,height=800dp,dpi=480",
    showBackground = true
)
annotation class DevicesPreview
