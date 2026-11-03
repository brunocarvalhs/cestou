package br.com.brunocarvalhs.howmuch.feature.shopping.domain.usecase

import br.com.brunocarvalhs.howmuch.core.domain.model.Shopping
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryMonth
import br.com.brunocarvalhs.howmuch.feature.shopping.domain.model.HistoryPurchase
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

internal val TestZone: ZoneId = ZoneId.of("America/Sao_Paulo")
internal val TestClock: Clock = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), TestZone)

internal fun millisOf(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
    LocalDateTime.of(year, month, day, hour, minute).atZone(TestZone).toInstant().toEpochMilli()

internal fun finishedShopping(
    id: String,
    price: Double = 100.0,
    finishedAt: Long? = null,
    createdAt: Long = 0L,
    status: Shopping.Status = Shopping.Status.FINISH
) = Shopping(
    id = id,
    title = "List $id",
    description = "",
    price = price,
    status = status,
    users = listOf("u1"),
    roles = emptyMap(),
    createdAt = createdAt,
    finishedAt = finishedAt
)

internal fun historyMonth(year: Int, month: Int, vararg ids: String, price: Double = 100.0) = HistoryMonth(
    key = YearMonth.of(year, month),
    total = price * ids.size,
    purchases = ids.map { id ->
        val shopping = finishedShopping(id, price, finishedAt = millisOf(year, month, 10))
        HistoryPurchase(shopping, millisOf(year, month, 10), null, 1, price)
    }
)
