package io.github.langstudy.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import io.github.langstudy.data.model.PortfolioItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface PortfolioRepository {
    fun getPortfolioItems(userId: String, limit: Long = 10): Flow<List<PortfolioItem>>
    suspend fun addPortfolioItem(userId: String, item: PortfolioItem)
    suspend fun deletePortfolioItem(userId: String, id: String)
    suspend fun updatePortfolioItem(userId: String, item: PortfolioItem)
}

class FirestorePortfolioRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PortfolioRepository {

    private fun getCollection(userId: String) =
        firestore.collection("users").document(userId).collection("portfolio")

    override fun getPortfolioItems(userId: String, limit: Long): Flow<List<PortfolioItem>> =
        callbackFlow {
            if (userId.isBlank()) {
                trySend(emptyList())
                awaitClose { }
                return@callbackFlow
            }
            val query = getCollection(userId)
                .orderBy("dateAdded", Query.Direction.DESCENDING)
                .limit(limit)

            val listener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirestoreRepo", "Error fetching portfolio for user $userId", error)
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val items = snapshot.toObjects(PortfolioItem::class.java)
                    trySend(items)
                }
            }
            awaitClose { listener.remove() }
        }

    override suspend fun addPortfolioItem(userId: String, item: PortfolioItem) {
        if (userId.isBlank()) return
        val data = hashMapOf(
            "title" to item.title,
            "link" to item.link,
            "type" to item.type,
            "videoId" to item.videoId,
            "isTop" to item.isTop,
            "isPrivate" to item.isPrivate,
            "language" to item.language,
            "dateAdded" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        if (item.id.isNotEmpty()) {
            getCollection(userId).document(item.id).set(data, SetOptions.merge()).await()
        } else {
            getCollection(userId).add(data).await()
        }
    }

    override suspend fun deletePortfolioItem(userId: String, id: String) {
        if (userId.isBlank() || id.isBlank()) return
        getCollection(userId).document(id).delete().await()
    }

    override suspend fun updatePortfolioItem(userId: String, item: PortfolioItem) {
        if (userId.isBlank() || item.id.isBlank()) return
        val updates = mutableMapOf<String, Any?>(
            "title" to item.title,
            "link" to item.link,
            "isTop" to item.isTop,
            "isPrivate" to item.isPrivate,
            "language" to item.language,
            "type" to item.type,
            "videoId" to item.videoId
        )
        getCollection(userId).document(item.id).update(updates).await()
    }

    private fun extractVideoId(url: String): String? {
        return io.github.langstudy.utils.UrlUtils.getYouTubeId(url)
    }
}

class MockPortfolioRepository : PortfolioRepository {
    private val mockData = mutableListOf<PortfolioItem>()
    override fun getPortfolioItems(userId: String, limit: Long): Flow<List<PortfolioItem>> =
        flow { emit(mockData.toList()) }

    override suspend fun addPortfolioItem(userId: String, item: PortfolioItem) {}
    override suspend fun deletePortfolioItem(userId: String, id: String) {}
    override suspend fun updatePortfolioItem(userId: String, item: PortfolioItem) {}
}
