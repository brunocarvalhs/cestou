package br.com.brunocarvalhs.howmuch.feature.products.data.model

import br.com.brunocarvalhs.howmuch.core.domain.model.ProductActivity
import kotlinx.serialization.Serializable

@Serializable
internal data class ProductActivityModel(
    val userId: String,
    val action: String,
    val timestamp: Long
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "action" to action,
        "timestamp" to timestamp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ProductActivityModel? {
            val userId = map["userId"] as? String ?: return null
            val action = map["action"] as? String ?: return null
            return ProductActivityModel(
                userId = userId,
                action = action,
                timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
            )
        }
    }
}

internal fun ProductActivityModel.toDomain(): ProductActivity? {
    val action = runCatching { ProductActivity.Action.valueOf(action) }.getOrNull() ?: return null
    return ProductActivity(userId = userId, action = action, timestamp = timestamp)
}

internal fun ProductActivity.toModel(): ProductActivityModel = ProductActivityModel(
    userId = userId,
    action = action.name,
    timestamp = timestamp
)
