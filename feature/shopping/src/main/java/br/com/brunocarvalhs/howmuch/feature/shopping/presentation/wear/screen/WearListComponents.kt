package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.wear.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.unit.Dp
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
private const val SKELETON_ALPHA = 0.15f

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

/**
 * Linha de esqueleto no mesmo formato de [WearListRow]: capa e duas barras cinzas (sdd-056).
 * Tem conteúdo com tamanho próprio de propósito: um `Box` vazio mede altura zero, então a linha
 * sumia e a lista não tinha o que rolar.
 */
@Composable
internal fun WearSkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(PillRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(CoverSize)
                .clip(RoundedCornerShape(CoverRadius))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = SKELETON_ALPHA))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SkeletonBar(width = 80.dp, height = 10.dp)
            SkeletonBar(width = 48.dp, height = 8.dp)
        }
    }
}

@Composable
private fun SkeletonBar(width: Dp, height: Dp) {
    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(PillRadius))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = SKELETON_ALPHA))
    )
}

/** Campo de busca simples: texto digitado, com dica quando vazio. */
@Composable
internal fun WearSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String = stringResource(R.string.shopping_wear_search_hint),
    tag: String = TAG_SEARCH_FIELD,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        modifier = modifier
            .clip(RoundedCornerShape(PillRadius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag(tag),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(text = hint)
                }
                inner()
            }
        }
    )
}
