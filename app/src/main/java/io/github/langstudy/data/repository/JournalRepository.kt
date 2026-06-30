package io.github.langstudy.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.github.langstudy.data.local.dao.JournalDao
import io.github.langstudy.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class JournalRepository(private val journalDao: JournalDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    val allEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()
    val entryCount: Flow<Int> = journalDao.getEntryCount()

    fun startSync(userId: String): Flow<Unit> = callbackFlow {
        if (userId.isBlank()) {
            awaitClose { }
            return@callbackFlow
        }
        listenerRegistration?.remove()

        val collectionRef = firestore.collection("users").document(userId)
            .collection("journal")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Close normally on error to prevent crash during sign out
                close()
                return@addSnapshotListener
            }

            snapshot?.let { querySnapshot ->
                launch {
                    val now = System.currentTimeMillis()
                    for (change in querySnapshot.documentChanges) {
                        val doc = change.document
                        when (change.type) {
                            DocumentChange.Type.ADDED,
                            DocumentChange.Type.MODIFIED -> {
                                val data = doc.data
                                val entry = JournalEntryEntity(
                                    id = doc.id,
                                    title = data["title"] as? String ?: "",
                                    content = data["content"] as? String ?: "",
                                    language = data["language"] as? String ?: "",
                                    timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: now
                                )
                                journalDao.insertEntry(entry)
                            }

                            DocumentChange.Type.REMOVED -> {
                                journalDao.deleteEntryById(doc.id)
                            }
                        }
                    }
                }
            }
            trySend(Unit)
        }

        listenerRegistration = listener
        awaitClose { listener.remove() }
    }

    suspend fun insert(entry: JournalEntryEntity, userId: String? = null) {
        journalDao.insertEntry(entry)
        userId?.let { uid ->
            pushToFirestore(uid, entry)
        }
    }

    suspend fun delete(entry: JournalEntryEntity, userId: String? = null) {
        journalDao.deleteEntry(entry)
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("journal").document(entry.id)
                .delete()
        }
    }

    private fun pushToFirestore(userId: String, entry: JournalEntryEntity) {
        val entryData = mapOf(
            "title" to entry.title,
            "content" to entry.content,
            "language" to entry.language,
            "timestamp" to com.google.firebase.Timestamp(java.util.Date(entry.timestamp))
        )

        firestore.collection("users").document(userId)
            .collection("journal").document(entry.id)
            .set(entryData, SetOptions.merge())
    }
}
