package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import coil.compose.AsyncImage

@Composable
internal fun IconProduct(
    iconUrl: String? = null,
    iconBackgroundColor: Color = Color(0xFFF0F0F0),
    emoji: String = Shopping.DEFAULT_EMOJI,
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(iconBackgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (iconUrl != null) {
            AsyncImage(
                model = iconUrl,
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Text(text = emoji, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
