package br.com.brunocarvalhs.howmuch.app.foundation.extensions

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

fun String?.toFormatDate(
    outputFormat: DateFormat,
    inputFormat: DateFormat = DateFormat.YEAR_MONTH_DAY,
): String {
    if (this.isNullOrEmpty()) return ""
    return try {
        val parser = SimpleDateFormat(inputFormat.pattern, Locale.getDefault())
        val date = parser.parse(this)
        val formatter = SimpleDateFormat(outputFormat.pattern, Locale.getDefault())
        formatter.format(date!!)
    } catch (_: Exception) {
        ""
    }
}

enum class DateFormat(val pattern: String) {
    DAY_MONTH_YEAR("dd/MM/yyyy"),
    YEAR_MONTH_DAY("yyyy-MM-dd"),
    MONTH_YEAR("MM/yyyy"),
    MONTH_NAME_YEAR("MMMM/yyyy"),
}

fun String?.isWithinLastDays(days: Int): Boolean {
    if (this.isNullOrEmpty()) return false
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(this) ?: error("Invalid date format")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        date.after(calendar.time)
    } catch (_: Exception) {
        false
    }
}

fun String?.toLocalDate(): LocalDate? {
    return try {
        this?.let { LocalDate.parse(it) }
    } catch (_: Exception) {
        null
    }
}
