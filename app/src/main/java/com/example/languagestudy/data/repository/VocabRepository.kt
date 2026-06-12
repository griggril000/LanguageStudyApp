package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.VocabDao
import com.example.languagestudy.data.local.entity.VocabEntity
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
                    val remoteVocab = querySnapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        VocabEntity(
                            id = doc.id,
                            word = data["word"] as? String ?: "",
                            translation = data["translation"] as? String ?: "",
                            category = data["category"] as? String ?: "General",
                            language = data["language"] as? String ?: "en",
                            dateAdded = (data["dateAdded"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: now
                        )
                    }

                    remoteVocab.forEach { vocab ->
                        vocabDao.insertVocab(vocab)
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
            "language" to vocab.language,
            "dateAdded" to com.google.firebase.Timestamp(java.util.Date(vocab.dateAdded))
        )

        firestore.collection("users").document(userId)
            .collection("vocabulary").document(vocab.id)
            .set(vocabData, SetOptions.merge())
    }
}
