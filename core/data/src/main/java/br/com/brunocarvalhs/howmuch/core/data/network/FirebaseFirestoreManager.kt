package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseFirestoreManager @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun execute(
        endpoint: String,
        method: NetworkService.Method,
        data: Map<String, Any?>? = null,
        query: Map<String, Any?>? = null
    ): Any? = try {
        when (method) {
            NetworkService.Method.GET -> get(endpoint, query)
            NetworkService.Method.POST -> post(endpoint, data)
            NetworkService.Method.PUT -> put(endpoint, data)
            NetworkService.Method.DELETE -> delete(endpoint)
        }
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (e: Exception) {
        throw NetworkService.NetworkException(message = e.message, cause = e)
    }

    private suspend fun get(
        endpoint: String,
        query: Map<String, Any?>?
    ): Any? {
        return try {
            if (isDocumentPath(endpoint)) {
                val snapshot = documentRef(endpoint).get().await()
                snapshot.data?.toMutableMap()?.apply {
                    put("id", snapshot.id)
                }
            } else {
                var ref: Query = collectionRef(endpoint)
                query?.forEach { (key, value) ->
                    ref = applyFilter(ref, key, value)
                }
                val snapshot = ref.get().await()
                snapshot.documents.mapNotNull { doc ->
                    doc.data?.toMutableMap()?.apply {
                        put("id", doc.id)
                    }
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw NetworkService.NetworkException(message = e.message, cause = e)
        }
    }

    private suspend fun post(
        endpoint: String,
        data: Map<String, Any?>?
    ): String {
        requireNotNull(data)

        return try {
            val id = data["id"]?.toString()

            if (id != null) {
                collectionRef(endpoint).document(id).set(data).await()
                id
            } else {
                collectionRef(endpoint).add(data).await().id
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw NetworkService.NetworkException(message = e.message, cause = e)
        }
    }

    private suspend fun put(
        endpoint: String,
        data: Map<String, Any?>?
    ): Boolean {
        require(isDocumentPath(endpoint)) {
            "PUT requer um endpoint de documento."
        }
        requireNotNull(data)

        return try {
            val updateData = data.filterKeys { it != "id" }.filterValues { it != null }
                .mapValues { (key, value) ->
                    if (key in ARRAY_FIELDS && value is List<*>) {
                        FieldValue.arrayUnion(*value.toTypedArray())
                    } else {
                        value
                    }
                }

            if (updateData.isNotEmpty()) {
                documentRef(endpoint).update(updateData).await()
            }
            true
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            throw NetworkService.NetworkException(message = e.message, cause = e)
        }
    }

    private suspend fun delete(
        endpoint: String
    ): Boolean {

        require(isDocumentPath(endpoint)) {
            "DELETE requer um endpoint de documento."
        }

        documentRef(endpoint).delete().await()

        return true
    }

    private fun applyFilter(
        ref: Query,
        key: String,
        value: Any?
    ): Query {

        return when {
            ARRAY_FIELDS.contains(key) -> ref.whereArrayContains(key, value!!)

            key.endsWith("_contains") -> ref.whereArrayContains(
                key.removeSuffix("_contains"), value!!
            )

            value is List<*> -> ref.whereIn(key, value)

            else -> ref.whereEqualTo(key, value)
        }
    }

    /**
     * shopping
     * shopping/{id}/products
     * shopping/{id}/products/{id}/prices
     */
    private fun collectionRef(endpoint: String): CollectionReference {

        val parts = endpoint.split("/")

        require(parts.size % 2 == 1) {
            "O endpoint deve terminar em uma coleção."
        }

        var collection = firestore.collection(parts[0])

        var i = 1

        while (i < parts.size) {

            val document = collection.document(parts[i])

            if (i + 1 < parts.size) {
                collection = document.collection(parts[i + 1])
            }

            i += 2
        }

        return collection
    }

    /**
     * shopping/{id}
     * shopping/{id}/products/{id}
     * shopping/{id}/products/{id}/prices/{id}
     */
    private fun documentRef(endpoint: String): DocumentReference {

        val parts = endpoint.split("/")

        require(parts.size % 2 == 0) {
            "O endpoint deve terminar em um documento."
        }

        var document = firestore.collection(parts[0]).document(parts[1])

        var i = 2

        while (i < parts.size) {
            document = document.collection(parts[i]).document(parts[i + 1])

            i += 2
        }

        return document
    }

    private fun isDocumentPath(endpoint: String): Boolean = endpoint.split("/").size % 2 == 0

    fun observe(
        endpoint: String,
        query: Map<String, Any?>? = null
    ): Flow<Any?> = callbackFlow {
        val registration = if (isDocumentPath(endpoint)) {
            documentRef(endpoint).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val data = snapshot.data?.toMutableMap()?.apply {
                        put("id", snapshot.id)
                    }
                    trySend(data)
                }
            }
        } else {
            var ref: Query = collectionRef(endpoint)
            query?.forEach { (key, value) ->
                ref = applyFilter(ref, key, value)
            }
            ref.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val data = snapshot.documents.mapNotNull { doc ->
                        doc.data?.toMutableMap()?.apply {
                            put("id", doc.id)
                        }
                    }
                    trySend(data)
                }
            }
        }
        awaitClose { registration.remove() }
    }

    companion object {
        private val ARRAY_FIELDS = setOf(
            "users", "roles", "products"
        )
    }
}
