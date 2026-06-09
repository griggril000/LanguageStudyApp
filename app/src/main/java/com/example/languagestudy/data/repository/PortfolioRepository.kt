package com.example.languagestudy.data.repository

import android.util.Log
import com.example.languagestudy.data.model.PortfolioItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
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

    override fun getPortfolioItems(userId: String, limit: Long): Flow<List<PortfolioItem>> = callbackFlow {
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
        val type = if (item.link.contains("soundcloud")) "soundcloud" else "youtube"
        val videoId = if (type == "youtube") extractVideoId(item.link) else null
        
        val data = hashMapOf(
            "title" to item.title,
            "link" to item.link,
            "type" to type,
            "videoId" to videoId,
            "isTop" to item.isTop,
            "isPrivate" to item.isPrivate,
            "language" to item.language,
            "dateAdded" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        getCollection(userId).add(data).await()
    }

    override suspend fun deletePortfolioItem(userId: String, id: String) {
        getCollection(userId).document(id).delete().await()
    }

    override suspend fun updatePortfolioItem(userId: String, item: PortfolioItem) {
        // Use set with SetOptions.merge() to avoid deleting fields the app doesn't know about
        getCollection(userId).document(item.id).set(item, SetOptions.merge()).await()
    }

    private fun extractVideoId(url: String): String? {
        val regex = "(?:youtube(?:-nocookie)?\\.com\\/(?:.*[?&]v=|v\\/|shorts\\/)|youtu\\.be\\/)([\\w-]{11})".toRegex()
        return regex.find(url)?.groupValues?.get(1)
    }
}

class MockPortfolioRepository : PortfolioRepository {
    private val mockData = mutableListOf<PortfolioItem>()
    override fun getPortfolioItems(userId: String, limit: Long): Flow<List<PortfolioItem>> = flow { emit(mockData.toList()) }
    override suspend fun addPortfolioItem(userId: String, item: PortfolioItem) {}
    override suspend fun deletePortfolioItem(userId: String, id: String) {}
    override suspend fun updatePortfolioItem(userId: String, item: PortfolioItem) {}
}
