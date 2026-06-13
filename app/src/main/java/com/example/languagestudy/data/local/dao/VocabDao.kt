package com.example.languagestudy.data.local.dao

import androidx.room.*
import com.example.languagestudy.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocabulary ORDER BY dateAdded DESC")
    fun getAllVocab(): Flow<List<VocabEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocab(vocab: VocabEntity)

    @Delete
    suspend fun deleteVocab(vocab: VocabEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabById(id: String)
}
