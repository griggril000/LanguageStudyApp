package io.github.langstudy.data.repository

import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import io.github.langstudy.data.local.dao.CategoryDao
import io.github.langstudy.data.local.dao.VocabDao
import io.github.langstudy.data.local.entity.CategoryEntity
import io.github.langstudy.data.local.entity.VocabEntity
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class VocabRepository(
    private val vocabDao: VocabDao,
    private val categoryDao: CategoryDao
) {
    private val firestore = FirebaseFirestore.getInstance()
    private var listenerRegistration: ListenerRegistration? = null
    private var categoryListenerRegistration: ListenerRegistration? = null

    val allVocab: Flow<List<VocabEntity>> = vocabDao.getAllVocab()
    val vocabCount: Flow<Int> = vocabDao.getVocabCount()
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()

    fun startSync(userId: String): Flow<Unit> = callbackFlow {
        if (userId.isBlank()) {
            awaitClose { }
            return@callbackFlow
        }
        listenerRegistration?.remove()
        categoryListenerRegistration?.remove()

        val vocabCollectionRef = firestore.collection("users").document(userId)
            .collection("vocabulary")

        val vocabListener = vocabCollectionRef.addSnapshotListener { snapshot, error ->
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
                                val category = data["category"] as? String ?: "General"
                                val vocab = VocabEntity(
                                    id = doc.id,
                                    word = data["word"] as? String ?: "",
                                    translation = data["translation"] as? String ?: "",
                                    category = category,
                                    status = (data["status"] as? String
                                        ?: "NOT_STARTED").uppercase(),
                                    language = data["language"] as? String ?: "en",
                                    dateAdded = (data["dateAdded"] as? com.google.firebase.Timestamp)?.toDate()?.time
                                        ?: now
                                )
                                vocabDao.insertVocab(vocab)
                                if (category != "General") {
                                    categoryDao.insertCategory(CategoryEntity(category))
                                }
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

        val categoryDocRef = firestore.collection("users").document(userId)
            .collection("metadata").document("categories")

        val categoryListener = categoryDocRef.addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            snapshot?.let { doc ->
                if (doc.exists()) {
                    val list = doc.get("list") as? List<String> ?: emptyList()
                    launch {
                        // To sync correctly, we ensure local reflects the remote list exactly.
                        // We could find diffs, but clearing and re-inserting is safe for this small list.
                        categoryDao.deleteAllCategories()
                        for (catName in list) {
                            categoryDao.insertCategory(CategoryEntity(catName))
                        }
                    }
                }
            }
            trySend(Unit)
        }

        listenerRegistration = vocabListener
        categoryListenerRegistration = categoryListener
        awaitClose {
            vocabListener.remove()
            categoryListener.remove()
        }
    }

    suspend fun insert(vocab: VocabEntity, userId: String? = null) {
        vocabDao.insertVocab(vocab)
        if (!userId.isNullOrBlank()) {
            pushToFirestore(userId, vocab)
        }
    }

    suspend fun update(vocab: VocabEntity, userId: String? = null) {
        vocabDao.insertVocab(vocab)
        if (!userId.isNullOrBlank()) {
            pushToFirestore(userId, vocab)
        }
    }

    suspend fun delete(vocab: VocabEntity, userId: String? = null) {
        vocabDao.deleteVocab(vocab)
        if (!userId.isNullOrBlank()) {
            firestore.collection("users").document(userId)
                .collection("vocabulary").document(vocab.id)
                .delete()
        }
    }

    suspend fun addCategory(name: String, userId: String? = null) {
        if (name.isBlank()) return
        categoryDao.insertCategory(CategoryEntity(name))
        userId?.let { uid ->
            val docRef = firestore.collection("users").document(uid)
                .collection("metadata").document("categories")

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val list = if (snapshot.exists()) {
                    (snapshot.get("list") as? List<String> ?: emptyList()).toMutableList()
                } else {
                    mutableListOf("General")
                }

                if (!list.contains(name)) {
                    list.add(name)
                    transaction.set(docRef, mapOf("list" to list), SetOptions.merge())
                }
            }.await()
        }
    }

    suspend fun deleteCategory(name: String, userId: String? = null) {
        if (name == "General") return
        categoryDao.deleteCategoryByName(name)
        vocabDao.deleteByCategory(name)
        userId?.let { uid ->
            val docRef = firestore.collection("users").document(uid)
                .collection("metadata").document("categories")

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (snapshot.exists()) {
                    val list =
                        (snapshot.get("list") as? List<String> ?: emptyList()).toMutableList()
                    if (list.remove(name)) {
                        transaction.set(docRef, mapOf("list" to list), SetOptions.merge())
                    }
                }
            }.await()

            try {
                val vocabInCat = firestore.collection("users").document(uid)
                    .collection("vocabulary").whereEqualTo("category", name).get().await()

                if (!vocabInCat.isEmpty) {
                    val batch = firestore.batch()
                    for (doc in vocabInCat.documents) {
                        batch.delete(doc.reference)
                    }
                    batch.commit().await()
                }
            } catch (e: Exception) {
                // Log or handle error
            }
        }
    }

    private fun pushToFirestore(userId: String, vocab: VocabEntity) {
        if (userId.isBlank()) return
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
