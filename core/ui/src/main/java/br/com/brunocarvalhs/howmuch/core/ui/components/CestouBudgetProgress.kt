package br.com.brunocarvalhs.howmuch.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetLevel
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.theme.Warning
import br.com.brunocarvalhs.howmuch.core.ui.R
import br.com.brunocarvalhs.howmuch.core.ui.extensions.formatPrice
import br.com.brunocarvalhs.howmuch.core.ui.utils.LocalCurrency

private const val PERCENT = 100
private const val BAR_HEIGHT_DP = 6
private val BarShape = RoundedCornerShape(percent = 50)

/**
 * Linha fina de progresso de um orçamento ou meta, com a legenda embaixo (porcentagem e valor
 * restante). A faixa aparece na cor e no texto, nunca só na cor. Sem limite válido não desenha nada.
 */
@Composable
fun CestouBudgetProgress(
    progress: BudgetProgress,
    caption: String,
    modifier: Modifier = Modifier
) {
    val ratio = progress.ratio ?: return
    val color = progress.level.color()

    Column(modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        LinearProgressIndicator(
            progress = { ratio.toFloat().coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(BAR_HEIGHT_DP.dp)
                .clip(BarShape),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.labelMedium,
            color = if (progress.level == BudgetLevel.OK) MaterialTheme.colorScheme.onSurfaceVariant else color
        )
    }
}

/** Legenda do orçamento de uma lista: "62% do orçamento · restam R$ 150,00" ou "Orçamento estourado em R$ 20,00". */
@Composable
fun budgetListCaption(progress: BudgetProgress): String {
    val currency = LocalCurrency.current
    val remaining = progress.remaining
    return when {
        remaining == null -> ""
        remaining < 0.0 -> stringResource(R.string.budget_caption_list_exceeded, (-remaining).formatPrice(currency))
        else -> stringResource(R.string.budget_caption_list_left, percentOf(progress), remaining.formatPrice(currency))
    }
}

/** Legenda da meta do mês: "Meta do mês: 45% · R$ 450,00 de R$ 1.000,00" ou "Meta do mês estourada em R$ 20,00". */
@Composable
fun budgetMonthCaption(progress: BudgetProgress): String {
    val currency = LocalCurrency.current
    val remaining = progress.remaining
    val limit = progress.limit
    return when {
        remaining == null || limit == null -> ""
        remaining < 0.0 -> stringResource(R.string.budget_caption_month_exceeded, (-remaining).formatPrice(currency))
        else -> stringResource(
            R.string.budget_caption_month,
            percentOf(progress),
            progress.spent.formatPrice(currency),
            limit.formatPrice(currency)
        )
    }
}

private fun percentOf(progress: BudgetProgress): Int = ((progress.ratio ?: 0.0) * PERCENT).toInt()

@Composable
private fun BudgetLevel.color() = when (this) {
    BudgetLevel.WARNING -> Warning
    BudgetLevel.EXCEEDED -> MaterialTheme.colorScheme.error
    BudgetLevel.OK, BudgetLevel.NONE -> MaterialTheme.colorScheme.primary
}
