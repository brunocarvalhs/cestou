package br.com.brunocarvalhs.howmuch.app.modules.history.components

import androidx.annotation.StringRes
import br.com.brunocarvalhs.howmuch.R

private const val SEVEN_DAYS = 7
private const val THIRTY_DAYS = 30

enum class HistoryFilterType(@param:StringRes val displayName: Int, val value: Int = 0) {
    ALL(R.string.all),
    TODAY(R.string.today),
    LAST_SEVEN_DAYS(R.string.last_7_days, SEVEN_DAYS),
    LAST_THIRTY_DAYS(R.string.last_30_days, THIRTY_DAYS),
    CURRENT_MONTH(R.string.current_month)
}
