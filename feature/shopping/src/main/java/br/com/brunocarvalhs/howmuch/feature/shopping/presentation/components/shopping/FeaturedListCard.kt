package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalCoral
import br.com.brunocarvalhs.howmuch.core.theme.SubtotalNight
import br.com.brunocarvalhs.howmuch.core.ui.extensions.CurrencyFormatter
import br.com.brunocarvalhs.howmuch.feature.shopping.R

/**
 * Card azul da lista em destaque na tela inicial (canvas "Subtotal — nova identidade"): mostra o
 * gasto contra o orçamento da lista e o quanto falta. Não aparece sem orçamento definido.
 */
@Composable
internal fun FeaturedListCard(
    shopping: Shopping,
    currencyFormatter: CurrencyFormatter,
    modifier: Modifier = Modifier
) {
    val budget = shopping.budget ?: return
    if (budget <= 0.0) return

    val spent = shopping.price
    val progress = (spent / budget).toFloat().coerceIn(0f, 1f)
    val exceeded = spent > budget
    val nearLimit = !exceeded && progress >= NEAR_LIMIT_RATIO
    val peopleCount = shopping.users.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(20.dp)
            .testTag("home_featured_list_card"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = shopping.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (peopleCount > 0) {
                Chip(
                    text = pluralStringResource(
                        R.plurals.shopping_featured_people,
                        peopleCount,
                        peopleCount
                    ),
                    background = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                )
            }
        }

        Column {
            Text(
                text = currencyFormatter.format(spent),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = Color.White
            )
            Text(
                text = stringResource(
                    R.string.shopping_featured_budget_of,
                    currencyFormatter.format(budget)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.28f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (exceeded) {
                    stringResource(R.string.shopping_featured_over, currencyFormatter.format(spent - budget))
                } else {
                    stringResource(R.string.shopping_featured_remaining, currencyFormatter.format(budget - spent))
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
            if (exceeded || nearLimit) {
                Chip(
                    text = stringResource(
                        if (exceeded) R.string.shopping_featured_exceeded else R.string.shopping_featured_near_limit
                    ),
                    background = SubtotalCoral,
                    contentColor = SubtotalNight
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, background: Color, contentColor: Color) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = contentColor
    )
}

private const val NEAR_LIMIT_RATIO = 0.8f
