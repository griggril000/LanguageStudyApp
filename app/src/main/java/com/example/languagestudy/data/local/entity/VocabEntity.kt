package com.example.languagestudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vocabulary")
data class VocabEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val word: String,
    val translation: String,
    val category: String,
    val language: String = "en", // Default to English or current study language
    val dateAdded: Long = System.currentTimeMillis()
)
