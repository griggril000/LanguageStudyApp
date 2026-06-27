package io.github.languagestudy.data.local.dao

import androidx.room.*
import io.github.languagestudy.data.local.entity.JournalEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT COUNT(*) FROM journal_entries")
    fun getEntryCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)
}
