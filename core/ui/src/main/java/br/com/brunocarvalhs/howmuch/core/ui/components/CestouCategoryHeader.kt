package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextSecondary
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory

/**
 * [expanded]/[onToggle] default to "always expanded, no-op" so existing callers that don't care
 * about collapsing keep behaving exactly as before — only [br.com.brunocarvalhs.howmuch.feature.cart.presentation.screen.CartScreen]
 * currently wires real collapse state through.
 */
@Composable
fun CestouCategoryHeader(
    category: String,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    onToggle: () -> Unit = {}
) {
    val productCategory = remember(category) { ProductCategory.fromString(category) }
    val locale = LocalLocale.current.platformLocale
    val rotation by animateFloatAsState(targetValue = if (expanded) 0f else -90f, label = "categoryChevron")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(productCategory.color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = productCategory.icon,
                contentDescription = null,
                tint = productCategory.color,
                modifier = Modifier.size(16.dp)
            )
        }

        Text(
            text = stringResource(productCategory.displayNameRes).uppercase(locale),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            color = CestouTextSecondary,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = CestouTextSecondary,
            modifier = Modifier.rotate(rotation)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CestouCategoryHeaderPreview() {
    MaterialTheme {
        CestouCategoryHeader(category = "Hortifruti")
    }
}

@Preview(showBackground = true, name = "Colapsada")
@Composable
private fun CestouCategoryHeaderCollapsedPreview() {
    MaterialTheme {
        CestouCategoryHeader(category = "Hortifruti", expanded = false)
    }
}
