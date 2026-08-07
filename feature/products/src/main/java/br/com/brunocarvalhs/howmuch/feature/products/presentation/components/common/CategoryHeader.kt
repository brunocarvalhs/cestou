package br.com.brunocarvalhs.howmuch.feature.products.presentation.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory

private const val BACKGROUND_ALPHA = 0.15f

@Composable
internal fun CategoryHeader(category: String) {
    val productCategory = remember(category) { ProductCategory.fromString(category) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    productCategory.color.copy(alpha = BACKGROUND_ALPHA),
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = productCategory.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = productCategory.color
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(productCategory.displayNameRes),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = productCategory.color
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryHeaderPreview() {
    MaterialTheme {
        CategoryHeader(category = "Hortifruti")
    }
}
