package io.github.languagestudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val language: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
