package br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Text
import androidx.wear.compose.ui.tooling.preview.WearPreviewDevices
import br.com.brunocarvalhs.howmuch.core.navigation.Navigator
import br.com.brunocarvalhs.howmuch.core.navigation.mobile.PairingWear
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouButton
import br.com.brunocarvalhs.howmuch.feature.auth.R
import br.com.brunocarvalhs.howmuch.feature.auth.presentation.wear.WearScrollableScreen

private const val TAG_LINK_PHONE = "wear_link_phone"

/**
 * Entry of the watch pairing: explains what the user must have at hand, then starts the flow.
 * The steps come before the button, so the action makes sense before it is pressed.
 */
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
        Box(
            modifier = Modifier
                .size(BADGE_SIZE)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(BADGE_CORNER)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(ICON_SIZE)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.wear_link_phone_title),
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))
        Step(number = 1, text = stringResource(R.string.wear_link_phone_step_1))
        Step(number = 2, text = stringResource(R.string.wear_link_phone_step_2))
        Step(number = 3, text = stringResource(R.string.wear_link_phone_step_3))
        Spacer(modifier = Modifier.height(10.dp))
        CestouButton(
            text = stringResource(R.string.wear_link_phone),
            onClick = onLinkClick,
            modifier = Modifier.fillMaxWidth().testTag(TAG_LINK_PHONE)
        )
    }
}

/** One numbered instruction: a bold number, then a short sentence. */
@Composable
private fun Step(number: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val BADGE_SIZE = 56.dp
private val BADGE_CORNER = 20.dp
private val ICON_SIZE = 28.dp

@WearPreviewDevices
@Composable
private fun LinkPhoneWearScreenPreview() {
    MaterialTheme {
        LinkPhoneWearContent(onLinkClick = {})
    }
}
