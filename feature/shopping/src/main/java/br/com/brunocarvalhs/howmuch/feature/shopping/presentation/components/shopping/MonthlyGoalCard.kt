package br.com.brunocarvalhs.howmuch.feature.shopping.presentation.components.shopping

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import br.com.brunocarvalhs.howmuch.core.domain.budget.BudgetProgress
import br.com.brunocarvalhs.howmuch.core.theme.CestouTheme
import br.com.brunocarvalhs.howmuch.core.ui.components.CestouBudgetProgress
import br.com.brunocarvalhs.howmuch.core.ui.components.budgetMonthCaption
import br.com.brunocarvalhs.howmuch.feature.shopping.R

/** Quanto da meta mensal já foi gasto nas compras finalizadas do mês; aparece só com meta definida. */
@Composable
internal fun MonthlyGoalCard(progress: BudgetProgress, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().testTag("home_monthly_goal_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.shopping_monthly_goal_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            CestouBudgetProgress(
                progress = progress,
                caption = budgetMonthCaption(progress),
                modifier = Modifier.testTag("home_monthly_goal_progress")
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MonthlyGoalCardPreview() {
    CestouTheme {
        MonthlyGoalCard(progress = BudgetProgress(limit = 1000.0, spent = 850.0))
    }
}
