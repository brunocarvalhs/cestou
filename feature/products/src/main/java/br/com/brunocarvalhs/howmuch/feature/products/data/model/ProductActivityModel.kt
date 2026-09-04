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
        fun fromMap(map: Map<String, Any?>): ProductActivityModel = ProductActivityModel(
            userId = map["userId"] as String,
            action = map["action"] as String,
            timestamp = (map["timestamp"] as? Number)?.toLong() ?: 0L
        )
    }
}

internal fun ProductActivityModel.toDomain(): ProductActivity = ProductActivity(
    userId = userId,
    action = ProductActivity.Action.valueOf(action),
    timestamp = timestamp
)

internal fun ProductActivity.toModel(): ProductActivityModel = ProductActivityModel(
    userId = userId,
    action = action.name,
    timestamp = timestamp
)
