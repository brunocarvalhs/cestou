package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.R as CoreR

@Composable
internal fun CaptureControls(
    onGalleryClick: () -> Unit,
    onCaptureClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(bottom = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.DarkGray.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = stringResource(CoreR.string.action_gallery),
                    tint = Color.White
                )
            }

            Surface(
                onClick = onCaptureClick,
                shape = CircleShape,
                color = Color.White,
                modifier = Modifier.size(80.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = stringResource(CoreR.string.action_take_photo),
                        modifier = Modifier.size(40.dp),
                        tint = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.size(56.dp))
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun CaptureControlsPreview() {
    CaptureControls(onGalleryClick = {}, onCaptureClick = {})
}
