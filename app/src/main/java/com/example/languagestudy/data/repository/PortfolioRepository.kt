package com.example.languagestudy.data.repository

import com.example.languagestudy.data.model.PortfolioItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface PortfolioRepository {
    fun getPortfolioItems(limit: Long = 10, lastTimestamp: Long? = null): Flow<List<PortfolioItem>>
    suspend fun addPortfolioItem(item: PortfolioItem)
    suspend fun deletePortfolioItem(id: String)
    suspend fun updatePortfolioItem(item: PortfolioItem)
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
