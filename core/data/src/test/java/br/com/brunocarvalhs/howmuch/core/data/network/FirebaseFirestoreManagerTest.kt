package br.com.brunocarvalhs.howmuch.core.data.network

import br.com.brunocarvalhs.howmuch.core.domain.services.NetworkService
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class FirebaseFirestoreManagerTest {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var documentRef: DocumentReference
    private lateinit var manager: FirebaseFirestoreManager

    @Before
    fun setup() {
        firestore = mockk()
        collectionRef = mockk()
        documentRef = mockk()
        every { firestore.collection("shopping") } returns collectionRef
        every { collectionRef.document(any()) } returns documentRef
        manager = FirebaseFirestoreManager(firestore)
    }

    @Test
    fun `execute GET on a document path returns its data with the id merged in`() = runTest {
        val snapshot = mockk<DocumentSnapshot> {
            every { data } returns mapOf("title" to "Weekly Groceries")
            every { id } returns "list-1"
        }
        every { documentRef.get() } returns Tasks.forResult(snapshot)

        val result = manager.execute("shopping/list-1", NetworkService.Method.GET)

        assertEquals(mapOf("title" to "Weekly Groceries", "id" to "list-1"), result)
    }

    @Test
    fun `execute GET on a collection path applies filters and merges each document id`() = runTest {
        val ref: Query = mockk()
        every { collectionRef.whereArrayContains("users", "user-1") } returns ref
        val doc1 = mockk<DocumentSnapshot> {
            every { data } returns mapOf("title" to "A")
            every { id } returns "1"
        }
        val doc2 = mockk<DocumentSnapshot> {
            every { data } returns mapOf("title" to "B")
            every { id } returns "2"
        }
        val snapshot = mockk<QuerySnapshot> { every { documents } returns listOf(doc1, doc2) }
        every { ref.get() } returns Tasks.forResult(snapshot)

        val result = manager.execute(
            "shopping",
            NetworkService.Method.GET,
            query = mapOf("users" to "user-1")
        )

        assertEquals(
            listOf(mapOf("title" to "A", "id" to "1"), mapOf("title" to "B", "id" to "2")),
            result
        )
    }

    @Test
    fun `execute GET on a collection path without a query hits the bare collection`() = runTest {
        val snapshot = mockk<QuerySnapshot> { every { documents } returns emptyList() }
        every { collectionRef.get() } returns Tasks.forResult(snapshot)

        val result = manager.execute("shopping", NetworkService.Method.GET)

        assertEquals(emptyList<Any>(), result)
    }

    @Test
    fun `execute POST with an explicit id sets the document at that id`() = runTest {
        every { documentRef.set(any()) } returns Tasks.forResult(null)
        val data = mapOf("id" to "list-1", "title" to "Weekly Groceries")

        val result = manager.execute("shopping", NetworkService.Method.POST, data = data)

        assertEquals("list-1", result)
        verify { collectionRef.document("list-1") }
        verify { documentRef.set(data) }
    }

    @Test
    fun `execute POST without an id lets Firestore generate one`() = runTest {
        val generatedRef = mockk<DocumentReference> { every { id } returns "generated-1" }
        every { collectionRef.add(any()) } returns Tasks.forResult(generatedRef)
        val data = mapOf("title" to "Weekly Groceries")

        val result = manager.execute("shopping", NetworkService.Method.POST, data = data)

        assertEquals("generated-1", result)
    }

    @Test
    fun `execute POST without data throws`() = runTest {
        try {
            manager.execute("shopping", NetworkService.Method.POST, data = null)
            fail("expected an exception")
        } catch (e: NetworkService.NetworkException) {
            // requireNotNull(data) is wrapped by the outer catch as a NetworkException
        }
    }

    @Test
    fun `execute PUT updates only non-null non-id fields`() = runTest {
        every { documentRef.update(any<Map<String, Any>>()) } returns Tasks.forResult(null)
        val data = mapOf("id" to "list-1", "title" to "Updated", "budget" to null)

        val result = manager.execute("shopping/list-1", NetworkService.Method.PUT, data = data)

        assertEquals(true, result)
        verify { documentRef.update(mapOf("title" to "Updated")) }
    }

    @Test
    fun `execute PUT skips the network call when there is nothing left to update`() = runTest {
        val data = mapOf("id" to "list-1", "budget" to null)

        val result = manager.execute("shopping/list-1", NetworkService.Method.PUT, data = data)

        assertEquals(true, result)
        verify(exactly = 0) { documentRef.update(any<Map<String, Any>>()) }
    }

    @Test
    fun `execute PUT on a collection path throws`() = runTest {
        try {
            manager.execute("shopping", NetworkService.Method.PUT, data = mapOf("title" to "x"))
            fail("expected an exception")
        } catch (e: NetworkService.NetworkException) {
            // require(isDocumentPath(endpoint)) fails and is wrapped
        }
    }

    @Test
    fun `execute DELETE removes the document`() = runTest {
        every { documentRef.delete() } returns Tasks.forResult(null)

        val result = manager.execute("shopping/list-1", NetworkService.Method.DELETE)

        assertEquals(true, result)
        verify { documentRef.delete() }
    }

    @Test
    fun `execute DELETE on a collection path throws`() = runTest {
        try {
            manager.execute("shopping", NetworkService.Method.DELETE)
            fail("expected an exception")
        } catch (e: NetworkService.NetworkException) {
            // require(isDocumentPath(endpoint)) fails and is wrapped
        }
    }

    @Test
    fun `execute wraps a failing Firestore call as a NetworkException`() = runTest {
        every { documentRef.get() } returns Tasks.forException(RuntimeException("offline"))

        try {
            manager.execute("shopping/list-1", NetworkService.Method.GET)
            fail("expected an exception")
        } catch (e: NetworkService.NetworkException) {
            assertEquals("offline", e.message)
        }
    }

    @Test
    fun `observe on a document path emits the latest snapshot with its id merged in`() = runTest {
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { documentRef.addSnapshotListener(capture(listenerSlot)) } returns registration

        val results = mutableListOf<Any?>()
        val job = launch { manager.observe("shopping/list-1").collect { results.add(it) } }
        testScheduler.advanceUntilIdle()

        val snapshot = mockk<DocumentSnapshot> {
            every { data } returns mapOf("title" to "Weekly Groceries")
            every { id } returns "list-1"
        }
        listenerSlot.captured.onEvent(snapshot, null)
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(mapOf("title" to "Weekly Groceries", "id" to "list-1")), results)
    }

    @Test
    fun `observe on a collection path emits every document with its id merged in`() = runTest {
        val listenerSlot = slot<EventListener<QuerySnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { collectionRef.addSnapshotListener(capture(listenerSlot)) } returns registration

        val results = mutableListOf<Any?>()
        val job = launch { manager.observe("shopping").collect { results.add(it) } }
        testScheduler.advanceUntilIdle()

        val doc = mockk<DocumentSnapshot> {
            every { data } returns mapOf("title" to "A")
            every { id } returns "1"
        }
        val snapshot = mockk<QuerySnapshot> { every { documents } returns listOf(doc) }
        listenerSlot.captured.onEvent(snapshot, null)
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(listOf(mapOf("title" to "A", "id" to "1"))), results)
    }

    @Test
    fun `observe closes the flow when Firestore reports an error`() = runTest {
        val listenerSlot = slot<EventListener<DocumentSnapshot>>()
        val registration = mockk<ListenerRegistration>(relaxed = true)
        every { documentRef.addSnapshotListener(capture(listenerSlot)) } returns registration

        var threw = false
        val job = launch {
            try {
                manager.observe("shopping/list-1").collect { }
            } catch (e: Exception) {
                threw = true
            }
        }
        testScheduler.advanceUntilIdle()

        val error = mockk<FirebaseFirestoreException>(relaxed = true)
        listenerSlot.captured.onEvent(null, error)
        testScheduler.advanceUntilIdle()
        job.cancel()

        assertTrue(threw)
    }
}
