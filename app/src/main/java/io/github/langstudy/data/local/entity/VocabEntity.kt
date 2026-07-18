package io.github.langstudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary")
data class VocabEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val word: String,
    val translation: String = "",
    val category: String = "General",
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, PROFICIENT
    val language: String = "en",
    val dateAdded: Long = System.currentTimeMillis(),
    val exampleSentence: String = ""
)
