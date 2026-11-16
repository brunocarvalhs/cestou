package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Botão quadrado de 44 dp do cabeçalho (nova identidade): contorno fino, ou preenchido em azul
 * quando [filled] é verdadeiro (ação principal, como "criar lista").
 */
@Composable
fun CestouHeaderButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    filled: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(start = 8.dp)
            .size(44.dp)
            .semantics { this.contentDescription = contentDescription }
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(14.dp),
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = if (filled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
