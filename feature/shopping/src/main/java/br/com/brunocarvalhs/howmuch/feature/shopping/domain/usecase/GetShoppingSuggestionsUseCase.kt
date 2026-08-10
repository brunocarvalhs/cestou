package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import android.content.Context
import br.com.brunocarvalhs.howmuch.feature.shopping.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

internal class GetShoppingSuggestionsUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Flow<List<String>> = flowOf(
        listOf(
            context.getString(R.string.shopping_suggestion_expensive),
            context.getString(R.string.shopping_suggestion_breakfast),
            context.getString(R.string.shopping_suggestion_pending),
            context.getString(R.string.shopping_suggestion_barbecue),
            context.getString(R.string.shopping_suggestion_spent_month)
        )
    )
}
