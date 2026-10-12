package br.com.brunocarvalhs.howmuch.core.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface NetworkService {

    suspend fun <T : Any> make(
        request: NetworkRequest,
        response: KClass<T>,
        responseType: KType? = null
    ): T?

    fun <T : Any> observe(
        request: NetworkRequest,
        response: KClass<T>,
        responseType: KType? = null
    ): Flow<T?>

    data class NetworkRequest(
        val endpoint: String,
        val payload: Map<String, Any?>? = null,
        val headers: Map<String, String>? = null,
        val query: Map<String, Any>? = null,
        val method: Method
    )

    /** Payload values for PUT that are sent as Firestore field transforms instead of plain values. */
    sealed interface FieldOp {
        data class ArrayRemove(val values: List<Any>) : FieldOp
        data object Delete : FieldOp
    }

    enum class Method {
        GET,
        POST,
        PUT,
        DELETE
    }

    data class NetworkException(
        val code: Int? = null,
        override val message: String? = null,
        override val cause: Throwable? = null
    ) : Exception(message, cause)
}

suspend inline fun <reified T : Any> NetworkService.make(
    request: NetworkService.NetworkRequest
): T? = make(request, T::class, typeOf<T>())

inline fun <reified T : Any> NetworkService.observe(
    request: NetworkService.NetworkRequest
): Flow<T?> = observe(request, T::class, typeOf<T>())

@Serializable
data class DocumentId(val id: String)

/**
 * Deletes [path] (a document) after deleting every document of each sub-collection in
 * [subCollections] (Firestore does not cascade). [NetworkService.make] swallows errors and returns
 * null, so null is treated as failure here and thrown, so callers never proceed with data left over.
 */
suspend fun NetworkService.deleteDocumentWithSubCollections(path: String, vararg subCollections: String) {
    subCollections.forEach { sub ->
        val docs = make<List<DocumentId>>(
            NetworkService.NetworkRequest(endpoint = "$path/$sub", method = NetworkService.Method.GET)
        ) ?: throw NetworkService.NetworkException(message = "Failed to list $path/$sub")
        docs.forEach { doc ->
            make<Boolean>(
                NetworkService.NetworkRequest(endpoint = "$path/$sub/${doc.id}", method = NetworkService.Method.DELETE)
            ) ?: throw NetworkService.NetworkException(message = "Failed to delete $path/$sub/${doc.id}")
        }
    }
    make<Boolean>(NetworkService.NetworkRequest(endpoint = path, method = NetworkService.Method.DELETE))
        ?: throw NetworkService.NetworkException(message = "Failed to delete $path")
}
