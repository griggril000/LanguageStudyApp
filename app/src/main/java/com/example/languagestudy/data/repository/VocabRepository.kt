package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.VocabDao
import com.example.languagestudy.data.local.entity.VocabEntity
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class VocabRepository(private val vocabDao: VocabDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    val allVocab: Flow<List<VocabEntity>> = vocabDao.getAllVocab()

    fun startSync(userId: String): Flow<Unit> = callbackFlow {
        listenerRegistration?.remove()
        
        val collectionRef = firestore.collection("users").document(userId)
            .collection("vocabulary")

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
                                val vocab = VocabEntity(
                                    id = doc.id,
                                    word = data["word"] as? String ?: "",
                                    translation = data["translation"] as? String ?: "",
                                    category = data["category"] as? String ?: "General",
                                    status = (data["status"] as? String ?: "NOT_STARTED").uppercase(),
                                    language = data["language"] as? String ?: "en",
                                    dateAdded = (data["dateAdded"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: now
                                )
                                vocabDao.insertVocab(vocab)
                            }
                            DocumentChange.Type.REMOVED -> {
                                vocabDao.deleteVocabById(doc.id)
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

    suspend fun insert(vocab: VocabEntity, userId: String? = null) {
        vocabDao.insertVocab(vocab)
        userId?.let { uid ->
            pushToFirestore(uid, vocab)
        }
    }

    suspend fun update(vocab: VocabEntity, userId: String? = null) {
        vocabDao.insertVocab(vocab) // Room @Insert(onConflict = REPLACE) handles update
        userId?.let { uid ->
            pushToFirestore(uid, vocab)
        }
    }

    suspend fun delete(vocab: VocabEntity, userId: String? = null) {
        vocabDao.deleteVocab(vocab)
        userId?.let { uid ->
            firestore.collection("users").document(uid)
                .collection("vocabulary").document(vocab.id)
                .delete()
        }
    }

    private fun pushToFirestore(userId: String, vocab: VocabEntity) {
        val vocabData = mapOf(
            "word" to vocab.word,
            "translation" to vocab.translation,
            "category" to vocab.category,
            "status" to vocab.status.lowercase(),
            "language" to vocab.language,
            "dateAdded" to com.google.firebase.Timestamp(java.util.Date(vocab.dateAdded))
        )

        firestore.collection("users").document(userId)
            .collection("vocabulary").document(vocab.id)
            .set(vocabData, SetOptions.merge())
    }
}
