package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import br.com.brunocarvalhs.howmuch.feature.shopping.presentation.state.CategoryShare
import kotlin.math.roundToInt

private const val PERCENT = 100
private val IconSize = 24.dp
private val BarHeight = 8.dp

@Composable
internal fun CategoryRankingRow(
    share: CategoryShare,
    index: Int,
    modifier: Modifier = Modifier
) {
    val others = share.category == null
    val name = stringResource(
        if (others) R.string.insights_category_others else share.category!!.displayNameRes
    )
    val color = share.category?.color ?: MaterialTheme.colorScheme.outline
    val value = share.total.formatPrice(LocalCurrency.current)
    val percent = "${(share.percent * PERCENT).roundToInt()}%"
    val description = stringResource(R.string.insights_category_description, name, value, percent)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("insights_category_$index")
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = (share.category ?: ProductCategory.OUTROS).icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(IconSize)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BarHeight)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(BarHeight))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(share.percent.coerceIn(0f, 1f))
                        .height(BarHeight)
                        .background(color, RoundedCornerShape(BarHeight))
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                text = percent,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
