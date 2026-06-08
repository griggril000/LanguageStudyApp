package com.example.languagestudy.data.repository

import com.example.languagestudy.data.model.PortfolioItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface PortfolioRepository {
    fun getPortfolioItems(limit: Long = 10, lastTimestamp: Long? = null): Flow<List<PortfolioItem>>
    suspend fun addPortfolioItem(item: PortfolioItem)
    suspend fun deletePortfolioItem(id: String)
    suspend fun updatePortfolioItem(item: PortfolioItem)
}

class FirestorePortfolioRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : PortfolioRepository {
    private val portfolioCollection = firestore.collection("portfolio")

    override fun getPortfolioItems(limit: Long, lastTimestamp: Long?): Flow<List<PortfolioItem>> = callbackFlow {
        var query = portfolioCollection
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)

        if (lastTimestamp != null) {
            query = query.startAfter(lastTimestamp)
        }

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
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

    override suspend fun addPortfolioItem(item: PortfolioItem) {
        portfolioCollection.add(item.copy(timestamp = System.currentTimeMillis())).await()
    }

    override suspend fun deletePortfolioItem(id: String) {
        portfolioCollection.document(id).delete().await()
    }

    override suspend fun updatePortfolioItem(item: PortfolioItem) {
        portfolioCollection.document(item.id).set(item).await()
    }
}

class MockPortfolioRepository : PortfolioRepository {
    private val mockData = mutableListOf(
        PortfolioItem("1", "Fingerspelling Sample - 2023", "https://www.youtube.com/watch?v=V6360lvihAI", true, System.currentTimeMillis()),
        PortfolioItem("2", "Timber Story", "https://www.youtube.com/watch?v=V6360lvihAI", false, System.currentTimeMillis() - 1000),
        PortfolioItem("3", "News - 2023", "https://youtu.be/pQqSs-dtdfM", false, System.currentTimeMillis() - 2000),
        PortfolioItem("4", "Sample Presentation", "https://www.youtube.com/watch?v=V6360lvihAI", false, System.currentTimeMillis() - 3000),
        PortfolioItem("5", "Language Journey", "https://youtu.be/pQqSs-dtdfM", true, System.currentTimeMillis() - 4000)
    )

    override fun getPortfolioItems(limit: Long, lastTimestamp: Long?): Flow<List<PortfolioItem>> = flow {
        delay(1000) // Simulate network delay
        emit(mockData.toList())
    }

    override suspend fun addPortfolioItem(item: PortfolioItem) {
        mockData.add(0, item.copy(id = (mockData.size + 1).toString(), timestamp = System.currentTimeMillis()))
    }

    override suspend fun deletePortfolioItem(id: String) {
        mockData.removeAll { it.id == id }
    }

    override suspend fun updatePortfolioItem(item: PortfolioItem) {
        val index = mockData.indexOfFirst { it.id == item.id }
        if (index != -1) {
            mockData[index] = item
        }
    }
}
