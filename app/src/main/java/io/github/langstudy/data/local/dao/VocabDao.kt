package io.github.langstudy.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.github.langstudy.data.local.entity.VocabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VocabDao {
    @Query("SELECT * FROM vocabulary ORDER BY dateAdded DESC")
    fun getAllVocab(): Flow<List<VocabEntity>>

    @Query("SELECT COUNT(*) FROM vocabulary")
    fun getVocabCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVocab(vocab: VocabEntity)

    @Delete
    suspend fun deleteVocab(vocab: VocabEntity)

    @Query("DELETE FROM vocabulary WHERE id = :id")
    suspend fun deleteVocabById(id: String)

    @Query("DELETE FROM vocabulary WHERE category = :category")
    suspend fun deleteByCategory(category: String)
}
