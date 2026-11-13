package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import br.com.brunocarvalhs.howmuch.feature.shopping.R

private val CoverSize = 36.dp
private val CoverRadius = 8.dp
private val PillRadius = 50.dp

/** Tag do campo de busca, para os testes de UI (AGENTS §5: sem asserts de texto). */
internal const val TAG_SEARCH_FIELD = "wear_search_field"

/**
 * Cor dos cards e pílulas: cinza neutro. O `Button` padrão do Wear M3 usa `primary` (verde),
 * então todo card precisa passar por aqui.
 */
@Composable
internal fun wearCardColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor = MaterialTheme.colorScheme.onSurface
)

/**
 * Linha de lista no padrão Spotify (sdd-056): capa quadrada arredondada com o emoji, título,
 * subtítulo opcional e marca de fixada. Os textos chegam prontos, então a linha não lê dados.
 */
@Composable
internal fun WearListRow(
    emoji: String,
    title: String,
    subtitle: String?,
    pinnedLabel: String?,
    onClick: () -> Unit,
    transformation: SurfaceTransformation,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = wearCardColors(),
        transformation = transformation
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(CoverSize)
                    .clip(RoundedCornerShape(CoverRadius))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                subtitle?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            pinnedLabel?.let {
                Text(
                    text = "📌",
                    modifier = Modifier.semantics { contentDescription = it }
                )
            }
        }
    }
}

/** Barra cinza de carregamento, no padrão de esqueleto do Spotify (sdd-056). */
@Composable
internal fun WearSkeletonRow(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(PillRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
    )
}

/** Campo de busca simples: texto digitado, com dica quando vazio. */
@Composable
internal fun WearSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(RoundedCornerShape(PillRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(TAG_SEARCH_FIELD),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = stringResource(R.string.shopping_wear_search_hint))
                }
                inner()
            }
        }
    )
}
