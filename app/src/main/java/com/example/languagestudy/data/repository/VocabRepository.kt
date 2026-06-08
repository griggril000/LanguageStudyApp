package com.example.languagestudy.data.repository

import com.example.languagestudy.data.local.dao.VocabDao
import com.example.languagestudy.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

class VocabRepository(private val vocabDao: VocabDao) {
    val allVocab: Flow<List<VocabEntity>> = vocabDao.getAllVocab()

    suspend fun insert(vocab: VocabEntity) {
        vocabDao.insertVocab(vocab)
    }

    suspend fun delete(vocab: VocabEntity) {
        vocabDao.deleteVocab(vocab)
    }
}
