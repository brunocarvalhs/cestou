package br.com.brunocarvalhs.howmuch.feature.shopping.domain.model

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import java.time.YearMonth

/**
 * A finished [shopping] list as the history shows it. [date] is `finishedAt ?: createdAt`, so lists
 * finished before `finishedAt` existed fall back to the month they were created in.
 */
internal data class HistoryPurchase(
    val shopping: Shopping,
    val date: Long,
    val establishment: String?,
    val peopleCount: Int,
    val total: Double
)

internal data class HistoryMonth(
    val key: YearMonth,
    val total: Double,
    val purchases: List<HistoryPurchase>
)
