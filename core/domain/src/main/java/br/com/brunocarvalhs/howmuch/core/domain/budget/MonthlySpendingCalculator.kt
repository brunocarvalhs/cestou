package br.com.brunocarvalhs.howmuch.core.domain.budget

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/**
 * Gasto do mês a partir das listas finalizadas. O mês de uma compra segue o histórico: a data de
 * finalização, ou a de criação nas listas antigas que não têm `finishedAt`.
 */
class MonthlySpendingCalculator @Inject constructor() {

    /**
     * Soma o `price` das listas finalizadas no mês de [reference], no fuso [zone].
     * [excludeId] deixa de fora a lista que está sendo vista, para somá-la como projeção.
     */
    fun finishedSpent(
        lists: List<Shopping>,
        reference: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        excludeId: String? = null
    ): Double {
        val month = YearMonth.from(reference.atZone(zone))
        return lists.asSequence()
            .filter { it.status == Shopping.Status.FINISH && it.id != excludeId }
            .filter { month == YearMonth.from(Instant.ofEpochMilli(it.finishedAt ?: it.createdAt).atZone(zone)) }
            .sumOf { it.price }
    }
}
