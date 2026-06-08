package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.JournalDao
import com.example.languagestudy.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

class JournalRepository(private val journalDao: JournalDao) {
    val allEntries: Flow<List<JournalEntryEntity>> = journalDao.getAllEntries()

    suspend fun insert(entry: JournalEntryEntity) {
        journalDao.insertEntry(entry)
    }

    suspend fun delete(entry: JournalEntryEntity) {
        journalDao.deleteEntry(entry)
    }
}
