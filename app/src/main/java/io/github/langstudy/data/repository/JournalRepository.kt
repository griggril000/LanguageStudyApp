package io.github.langstudy.data.repository

import android.content.Context
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.github.langstudy.data.local.dao.JournalDao
import io.github.langstudy.data.local.entity.JournalEntryEntity
import io.github.langstudy.data.model.JournalDraft
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class JournalRepository(
    private val journalDao: JournalDao,
    private val context: Context? = null
) {
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
                    mentorVisible = data["mentorVisible"] as? Boolean ?: false,
                    tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
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
                                    mentorVisible = data["mentorVisible"] as? Boolean ?: false,
                                    tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
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
            "mentorVisible" to entry.mentorVisible,
            "tags" to entry.tags
        )

        firestore.collection("users").document(userId)
            .collection("journal").document(entry.id)
            .set(entryData, SetOptions.merge())
    }

    private fun getDraftDocRef(userId: String) =
        firestore.collection("users").document(userId)
            .collection("journalDrafts").document("current")

    fun saveDraftLocal(userId: String, draft: JournalDraft) {
        if (userId.isBlank() || context == null) return
        val prefs = context.getSharedPreferences("journal_drafts_$userId", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("title", draft.title)
            .putString("content", draft.content)
            .putString("language", draft.language)
            .putString("editingId", draft.editingId)
            .putBoolean("mentorVisible", draft.mentorVisible)
            .putString("mentorAccessLevel", draft.mentorAccessLevel)
            .putStringSet("tags", draft.tags.toSet())
            .putLong("updatedAtMs", draft.updatedAtMs)
            .apply()
    }

    fun getDraftLocal(userId: String): JournalDraft? {
        if (userId.isBlank() || context == null) return null
        val prefs = context.getSharedPreferences("journal_drafts_$userId", Context.MODE_PRIVATE)
        if (!prefs.contains("updatedAtMs")) return null
        val title = prefs.getString("title", "") ?: ""
        val content = prefs.getString("content", "") ?: ""
        val language = prefs.getString("language", "") ?: ""
        val editingId = prefs.getString("editingId", "") ?: ""
        val mentorVisible = prefs.getBoolean("mentorVisible", false)
        val mentorAccessLevel = prefs.getString("mentorAccessLevel", "view") ?: "view"
        val tags = prefs.getStringSet("tags", emptySet())?.toList() ?: emptyList()
        val updatedAtMs = prefs.getLong("updatedAtMs", 0L)
        if (title.isBlank() && content.isBlank()) return null
        return JournalDraft(
            title = title,
            content = content,
            language = language,
            editingId = editingId,
            mentorVisible = mentorVisible,
            mentorAccessLevel = mentorAccessLevel,
            tags = tags,
            updatedAtMs = updatedAtMs
        )
    }

    fun clearDraftLocal(userId: String) {
        if (userId.isBlank() || context == null) return
        val prefs = context.getSharedPreferences("journal_drafts_$userId", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    suspend fun pushDraftToRemote(userId: String, draft: JournalDraft) {
        if (userId.isBlank()) return
        try {
            val draftData = mapOf(
                "title" to draft.title,
                "content" to draft.content,
                "language" to draft.language,
                "editingId" to draft.editingId,
                "mentorVisible" to draft.mentorVisible,
                "mentorAccessLevel" to draft.mentorAccessLevel,
                "tags" to draft.tags,
                "updatedAtMs" to draft.updatedAtMs
            )
            getDraftDocRef(userId).set(draftData, SetOptions.merge()).await()
        } catch (e: Exception) {
            // Ignore offline or transient network failures during auto-save
        }
    }

    suspend fun clearDraftRemote(userId: String) {
        if (userId.isBlank()) return
        try {
            getDraftDocRef(userId).delete().await()
        } catch (e: Exception) {
            // Ignore error
        }
    }

    suspend fun clearDraft(userId: String) {
        clearDraftLocal(userId)
        clearDraftRemote(userId)
    }

    fun observeDraft(userId: String): Flow<JournalDraft?> = callbackFlow {
        if (userId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val initialLocal = getDraftLocal(userId)
        if (initialLocal != null) {
            trySend(initialLocal)
        }

        val docRef = getDraftDocRef(userId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(getDraftLocal(userId))
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val data = snapshot.data
                if (data != null) {
                    val remoteDraft = JournalDraft(
                        title = data["title"] as? String ?: "",
                        content = data["content"] as? String ?: "",
                        language = data["language"] as? String ?: "",
                        editingId = data["editingId"] as? String ?: "",
                        mentorVisible = data["mentorVisible"] as? Boolean ?: false,
                        mentorAccessLevel = data["mentorAccessLevel"] as? String ?: "view",
                        tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                        updatedAtMs = (data["updatedAtMs"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                    val currentLocal = getDraftLocal(userId)
                    val bestDraft = if (currentLocal != null && currentLocal.updatedAtMs > remoteDraft.updatedAtMs) {
                        currentLocal
                    } else {
                        saveDraftLocal(userId, remoteDraft)
                        remoteDraft
                    }
                    trySend(bestDraft)
                } else {
                    trySend(getDraftLocal(userId))
                }
            } else {
                val local = getDraftLocal(userId)
                trySend(local)
            }
        }
        awaitClose { listener.remove() }
    }
}
