package br.com.brunocarvalhs.howmuch.core.data.network

import io.github.jan.supabase.SupabaseClient
import io.mockk.mockk
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Covers [SupabasePostgrestManager.resolveRoute] (endpoint -> table routing) and
 * [SupabasePostgrestManager.toRawValue] (Postgrest JSON -> the raw Map/List shape
 * [RawDataGateway] callers expect, matching what Firestore's SDK returns natively) in
 * isolation, without touching the network layer. Those two pieces of pure logic are
 * where a routing or JSON-shape mistake would silently corrupt data; the actual
 * Postgrest/Realtime wiring is exercised live during the pilot step instead, since
 * supabase-kt's `from`/`select`/`filter` DSL is extension-function-heavy and not
 * practical to mock in isolation.
 */
class SupabasePostgrestManagerTest {

    private lateinit var manager: SupabasePostgrestManager

    @Before
    fun setup() {
        manager = SupabasePostgrestManager(mockk(), CompatibilityConverter())
    }

    @Test
    fun `resolveRoute maps the shopping list collection and document endpoints`() {
        val collection = manager.resolveRoute("shopping") as SupabasePostgrestManager.Route.Table
        assertEquals("shopping_lists", collection.table)
        assertNull(collection.parentColumn)

        val document = manager.resolveRoute("shopping/list-1") as SupabasePostgrestManager.Route.Row
        assertEquals("shopping_lists", document.table)
        assertEquals("list-1", document.id)
    }

    @Test
    fun `resolveRoute maps nested product endpoints to shopping_list_products with the parent scope`() {
        val collection =
            manager.resolveRoute("shopping/list-1/products") as SupabasePostgrestManager.Route.Table
        assertEquals("shopping_list_products", collection.table)
        assertEquals("shopping_id", collection.parentColumn)
        assertEquals("list-1", collection.parentId)

        val document =
            manager.resolveRoute("shopping/list-1/products/prod-1") as SupabasePostgrestManager.Route.Row
        assertEquals("shopping_list_products", document.table)
        assertEquals("prod-1", document.id)
        assertEquals("shopping_id", document.parentColumn)
        assertEquals("list-1", document.parentId)
    }

    @Test
    fun `resolveRoute maps user profile and common-products endpoints`() {
        val profile = manager.resolveRoute("users/user-1") as SupabasePostgrestManager.Route.Row
        assertEquals("user_profiles", profile.table)
        assertEquals("user-1", profile.id)

        val collection =
            manager.resolveRoute("users/user-1/common-products") as SupabasePostgrestManager.Route.Table
        assertEquals("common_products", collection.table)
        assertEquals("user_id", collection.parentColumn)
        assertEquals("user-1", collection.parentId)

        val document =
            manager.resolveRoute("users/user-1/common-products/cp-1") as SupabasePostgrestManager.Route.Row
        assertEquals("common_products", document.table)
        assertEquals("cp-1", document.id)
        assertEquals("user_id", document.parentColumn)
        assertEquals("user-1", document.parentId)
    }

    @Test
    fun `resolveRoute maps the flat notifications collection and document endpoints`() {
        val collection = manager.resolveRoute("notifications") as SupabasePostgrestManager.Route.Table
        assertEquals("notifications", collection.table)

        val document = manager.resolveRoute("notifications/n-1") as SupabasePostgrestManager.Route.Row
        assertEquals("notifications", document.table)
        assertEquals("n-1", document.id)
    }

    @Test(expected = br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService.NetworkException::class)
    fun `resolveRoute throws for an endpoint shape the app doesn't use`() {
        manager.resolveRoute("shopping/list-1/products/prod-1/prices")
    }

    @Test
    fun `toRawValue converts a JsonObject into a plain Map with unwrapped primitives`() {
        val json = JsonObject(
            mapOf(
                "id" to JsonPrimitive("list-1"),
                "price" to JsonPrimitive(12.5),
                "position" to JsonPrimitive(3L),
                "isFavorite" to JsonPrimitive(true),
                "budget" to JsonNull,
                "users" to JsonArray(listOf(JsonPrimitive("u1"), JsonPrimitive("u2")))
            )
        )

        val result = with(manager) { json.toRawValue() }

        assertEquals("list-1", result["id"])
        assertEquals(12.5, result["price"])
        assertEquals(3L, result["position"])
        assertEquals(true, result["isFavorite"])
        assertNull(result["budget"])
        assertEquals(listOf("u1", "u2"), result["users"])
    }

    @Test
    fun `toRawValue converts nested JsonObjects and JsonArrays recursively`() {
        val json = JsonObject(
            mapOf(
                "roles" to JsonObject(mapOf("user-1" to JsonPrimitive("owner"))),
                "products" to JsonArray(
                    listOf(JsonObject(mapOf("id" to JsonPrimitive("p1"), "quantity" to JsonPrimitive(2L))))
                )
            )
        )

        val result = with(manager) { json.toRawValue() }

        assertEquals(mapOf("user-1" to "owner"), result["roles"])
        assertEquals(listOf(mapOf("id" to "p1", "quantity" to 2L)), result["products"])
    }
}
