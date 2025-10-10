package br.com.brunocarvalhs.data.services

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RealtimeService(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
        "https://how-much-2a72e-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ),
    val reference: DatabaseReference = database.reference
) {

    suspend fun <T> setValue(path: String, value: T) {
        reference.child(path).setValue(value).await()
    }

    suspend fun removeValue(path: String) {
        reference.child(path).removeValue().await()
    }

    suspend fun <T> getValue(path: String, clazz: Class<T>): T? {
        val snapshot = reference.child(path).get().await()
        return snapshot.getValue(clazz)
    }

    suspend fun <T> queryByChild(path: String, child: String, value: String, clazz: Class<T>): T? {
        val snapshot = reference.child(path)
            .orderByChild(child)
            .equalTo(value)
            .get()
            .await()
        return snapshot.children.firstOrNull()?.getValue(clazz)
    }

    fun <T> observe(path: String, clazz: Class<T>): Flow<T> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.getValue(clazz)?.let { trySend(it).isSuccess }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        reference.child(path).addValueEventListener(listener)
        awaitClose { reference.child(path).removeEventListener(listener) }
    }
}
