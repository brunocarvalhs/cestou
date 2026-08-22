package br.com.brunocarvalhs.howmuch.feature.products.app.presentation.components.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.ui.entity.ProductCategory
import br.com.brunocarvalhs.howmuch.core.theme.CestouTextSecondary
import java.util.Locale

@Composable
fun CategoryHeader(category: String) {
    val productCategory = remember(category) { ProductCategory.fromString(category) }

    Text(
        text = stringResource(productCategory.displayNameRes).uppercase(Locale.getDefault()),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = CestouTextSecondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun CategoryHeaderPreview() {
    MaterialTheme {
        CategoryHeader(category = "Hortifruti")
    }
}
