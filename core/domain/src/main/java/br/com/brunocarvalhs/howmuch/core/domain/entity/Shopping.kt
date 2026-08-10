package br.com.brunocarvalhs.howmuch.core.domain.entity

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Stable
@Serializable
data class Shopping(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val status: Status,
    val users: List<String>,
    val roles: Map<String, String>,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isCategorized: Boolean = true,
    val shortCode: String? = null,
    val budget: Double? = null,
    val position: Int = 0
) {
    @Serializable
    enum class Status {
        NEW,
        IN_PROGRESS,
        FINISH
    }
}
