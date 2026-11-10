package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingWear
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.WearScrollableScreen

private const val TAG_LINK_PHONE = "wear_link_phone"

@Composable
internal fun LinkPhoneWearScreen(
    navigator: Navigator
) {
    LinkPhoneWearContent(
        onLinkClick = {
            navigator.navigate(PairingWear::class.java.name)
        }
    )
}

@Composable
private fun LinkPhoneWearContent(
    onLinkClick: () -> Unit
) {
    WearScrollableScreen {
        item {
            CestouButton(
                text = stringResource(R.string.wear_link_phone),
                onClick = onLinkClick,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .testTag(TAG_LINK_PHONE)
            )
        }
    }
}

@WearPreviewDevices
@Composable
private fun LinkPhoneWearScreenPreview() {
    MaterialTheme {
        LinkPhoneWearContent(onLinkClick = {})
    }
}
