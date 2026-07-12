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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JournalRepository(private val journalDao: JournalDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    val allEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()
    val entryCount: Flow<Int> = journalDao.getEntryCount()

    suspend fun syncOneShot(userId: String) {
        if (userId.isBlank()) return
        try {
            val snapshot = firestore.collection("users").document(userId)
                .collection("journal").get().await()

            val now = System.currentTimeMillis()
            val remoteIds = mutableSetOf<String>()
            for (doc in snapshot.documents) {
                val data = doc.data ?: continue
                val entry = JournalEntryEntity(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    content = data["content"] as? String ?: "",
                    language = data["language"] as? String ?: "",
                    timestamp = (data["dateAdded"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: now,
                    dateModified = (data["dateModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                        ?: now,
                    mentorAccessLevel = data["mentorAccessLevel"] as? String ?: "view",
                    mentorVisible = data["mentorVisible"] as? Boolean ?: false
                )
                journalDao.insertEntry(entry)
                remoteIds.add(doc.id)
            }

            // Full Sync: Remove local items that are not in remote
            val allLocal = journalDao.getAllEntries().first()
            for (local in allLocal) {
                if (!remoteIds.contains(local.id)) {
                    journalDao.deleteEntry(local)
                }
            }
        } catch (e: Exception) {
            // Log error
        }
    }

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
                close(error)
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
                                    timestamp = (data["dateAdded"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: now,
                                    dateModified = (data["dateModified"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: now,
                                    mentorAccessLevel = data["mentorAccessLevel"] as? String ?: "view",
                                    mentorVisible = data["mentorVisible"] as? Boolean ?: false
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
        if (!userId.isNullOrBlank()) {
            pushToFirestore(userId, entry)
        }
    }

    suspend fun delete(entry: JournalEntryEntity, userId: String? = null) {
        journalDao.deleteEntry(entry)
        if (!userId.isNullOrBlank()) {
            firestore.collection("users").document(userId)
                .collection("journal").document(entry.id)
                .delete()
        }
    }

    private fun pushToFirestore(userId: String, entry: JournalEntryEntity) {
        if (userId.isBlank()) return
        val entryData = mapOf(
            "title" to entry.title,
            "content" to entry.content,
            "language" to entry.language,
            "dateAdded" to com.google.firebase.Timestamp(java.util.Date(entry.timestamp)),
            "dateModified" to com.google.firebase.Timestamp(java.util.Date(entry.dateModified)),
            "mentorAccessLevel" to entry.mentorAccessLevel,
            "mentorVisible" to entry.mentorVisible
        )

        firestore.collection("users").document(userId)
            .collection("journal").document(entry.id)
            .set(entryData, SetOptions.merge())
    }
}
