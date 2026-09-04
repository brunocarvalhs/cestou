package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.util.UUID
import javax.inject.Inject

/**
 * Supabase (Postgres/PostgREST) implementation of [RawDataGateway], the Supabase-side
 * counterpart to [FirebaseFirestoreManager] during/after the Firestore migration.
 *
 * Endpoint routing is a small explicit table rather than a generic path translator: the
 * app only ever uses the 5 shapes enumerated in [resolveRoute], matching how
 * [FirebaseFirestoreManager] itself hardcodes field names (`ARRAY_FIELDS`) rather than
 * being fully generic.
 */
class SupabasePostgrestManager @Inject constructor(
    private val supabase: SupabaseClient,
    private val compatibilityConverter: CompatibilityConverter
) : RawDataGateway {

    internal sealed class Route(val table: String, val parentColumn: String?, val parentId: String?) {
        class Table(table: String, parentColumn: String? = null, parentId: String? = null) :
            Route(table, parentColumn, parentId)

        class Row(table: String, val id: String, parentColumn: String? = null, parentId: String? = null) :
            Route(table, parentColumn, parentId)
    }

    internal fun resolveRoute(endpoint: String): Route {
        val p = endpoint.split("/")
        return when {
            p.size == 1 && p[0] == "shopping" -> Route.Table("shopping_lists")
            p.size == 2 && p[0] == "shopping" -> Route.Row("shopping_lists", p[1])
            p.size == 3 && p[0] == "shopping" && p[2] == "products" ->
                Route.Table("shopping_list_products", parentColumn = "shopping_id", parentId = p[1])
            p.size == 4 && p[0] == "shopping" && p[2] == "products" ->
                Route.Row("shopping_list_products", p[3], parentColumn = "shopping_id", parentId = p[1])
            p.size == 2 && p[0] == "users" -> Route.Row("user_profiles", p[1])
            p.size == 3 && p[0] == "users" && p[2] == "common-products" ->
                Route.Table("common_products", parentColumn = "user_id", parentId = p[1])
            p.size == 4 && p[0] == "users" && p[2] == "common-products" ->
                Route.Row("common_products", p[3], parentColumn = "user_id", parentId = p[1])
            p.size == 1 && p[0] == "notifications" -> Route.Table("notifications")
            p.size == 2 && p[0] == "notifications" -> Route.Row("notifications", p[1])
            else -> throw NetworkService.NetworkException(message = "Unmapped endpoint: $endpoint")
        }
    }

    override suspend fun execute(
        endpoint: String,
        method: NetworkService.Method,
        data: Map<String, Any?>?,
        query: Map<String, Any?>?
    ): Any? = try {
        when (method) {
            NetworkService.Method.GET -> get(resolveRoute(endpoint), query)
            NetworkService.Method.POST -> post(resolveRoute(endpoint), data)
            NetworkService.Method.PUT -> put(resolveRoute(endpoint), data)
            NetworkService.Method.DELETE -> delete(resolveRoute(endpoint))
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: NetworkService.NetworkException) {
        throw e
    } catch (e: Exception) {
        throw NetworkService.NetworkException(message = e.message, cause = e)
    }

    private suspend fun get(route: Route, query: Map<String, Any?>?): Any? = when (route) {
        is Route.Row -> supabase.from(route.table).select {
            filter {
                eq("id", route.id)
                applyParent(route)
            }
        }.decodeSingleOrNull<JsonObject>()?.toRawValue()

        is Route.Table -> supabase.from(route.table).select {
            filter {
                applyParent(route)
                query?.forEach { (key, value) -> applyFilter(key, value) }
            }
        }.decodeList<JsonObject>().map { it.toRawValue() }
    }

    private suspend fun post(route: Route, data: Map<String, Any?>?): String {
        requireNotNull(data)

        val id = data["id"]?.toString() ?: UUID.randomUUID().toString()
        val payload = (compatibilityConverter.toJsonElement(data + ("id" to id)) as? JsonObject)
            ?: JsonObject(emptyMap())

        supabase.from(route.table).insert(JsonArray(listOf(payload)))
        return id
    }

    private suspend fun put(route: Route, data: Map<String, Any?>?): Boolean {
        require(route is Route.Row) { "PUT requer um endpoint de documento." }
        requireNotNull(data)

        var updateData = data.filterKeys { it != "id" }.filterValues { it != null }
        if (updateData.isEmpty()) return true

        // Mirrors FieldValue.arrayUnion's semantics for the `users` text[] column via a
        // read-modify-write, since the equivalent Postgres RPC hasn't been created yet.
        // Not race-free under concurrent joins on the same list -- see the migration plan.
        val unionKey = updateData.keys.firstOrNull { it == "users" && updateData[it] is List<*> }
        if (unionKey != null && route.table == "shopping_lists") {
            val current = supabase.from(route.table).select {
                filter { eq("id", route.id) }
            }.decodeSingleOrNull<JsonObject>()?.toRawValue() as? Map<*, *>
            val currentUsers = (current?.get(unionKey) as? List<*>).orEmpty()
            val incoming = (updateData[unionKey] as List<*>)
            updateData = updateData + (unionKey to (currentUsers + incoming).distinct())
        }

        val payload = compatibilityConverter.toJsonElement(updateData)
        supabase.from(route.table).update(payload) {
            filter {
                eq("id", route.id)
                applyParent(route)
            }
        }
        return true
    }

    private suspend fun delete(route: Route): Boolean {
        require(route is Route.Row) { "DELETE requer um endpoint de documento." }

        supabase.from(route.table).delete {
            filter {
                eq("id", route.id)
                applyParent(route)
            }
        }
        return true
    }

    private fun PostgrestFilterBuilder.applyParent(route: Route) {
        val column = route.parentColumn ?: return
        val value = route.parentId ?: return
        eq(column, value)
    }

    private fun PostgrestFilterBuilder.applyFilter(key: String, value: Any?) {
        requireNotNull(value) { "Filter value for '$key' must not be null." }
        when {
            ARRAY_FIELDS.contains(key) -> contains(key, listOf(value))
            key.endsWith("_contains") -> contains(key.removeSuffix("_contains"), listOf(value))
            value is List<*> -> isIn(key, value.filterNotNull())
            else -> eq(key, value)
        }
    }

    override fun observe(endpoint: String, query: Map<String, Any?>?): Flow<Any?> = callbackFlow {
        val route = resolveRoute(endpoint)

        suspend fun emitCurrent() = send(get(route, query))

        emitCurrent()

        val channel = supabase.channel("raw-data-gateway-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = route.table
        }
        val collectJob = launch {
            changes.collect { emitCurrent() }
        }
        channel.subscribe()

        awaitClose {
            collectJob.cancel()
            launch { runCatching { channel.unsubscribe() } }
        }
    }

    internal fun JsonObject.toRawValue(): Map<String, Any?> =
        entries.associate { (key, value) -> key to value.toRawValue() }

    internal fun JsonElement.toRawValue(): Any? = when (this) {
        is JsonNull -> null
        is JsonObject -> toRawValue()
        is JsonArray -> map { it.toRawValue() }
        is JsonPrimitive -> when {
            isString -> content
            else -> booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
        }
    }

    companion object {
        private val ARRAY_FIELDS = setOf("users", "roles", "products")
    }
}
