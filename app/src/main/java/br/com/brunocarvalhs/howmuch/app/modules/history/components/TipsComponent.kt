package br.com.brunocarvalhs.howmuch.app.modules.history.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.R
import br.com.brunocarvalhs.howmuch.app.foundation.analytics.trackClick
import br.com.brunocarvalhs.howmuch.app.foundation.constants.TIPS
import br.com.brunocarvalhs.howmuch.app.foundation.constants.Tips
import br.com.brunocarvalhs.howmuch.app.foundation.extensions.openUrl

@Composable
fun TipsComponent(
    context: Context,
    tips: List<Tips> = emptyList(),
) {
    val shuffledTips = remember { tips.shuffled() }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(shuffledTips) {
            TipItem(tip = it, onClick = {
                context.openUrl(url = it.link)
            })
        }
    }
}

@Composable
private fun TipItem(
    tip: Tips,
    onClick: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.padding(4.dp),
        shape = MaterialTheme.shapes.medium,
        onClick = {
            onClick()
            trackClick(
                viewId = "tip_${tip.title}",
                viewName = "Tip: ${tip.title}",
                screenName = "HistoryScreen"
            )
        }
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
                .width(220.dp)
        ) {
            Text(
                text = tip.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 2,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = stringResource(R.string.click_to_read_more),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
@Preview
private fun TipsComponentPreview() {
    val context = LocalContext.current

    TipsComponent(
        context = context,
        tips = TIPS,
    )
}
