package com.example.languagestudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val progress: Int, // 0-100
    val level: String // Beginner, Intermediate, Advanced
)
