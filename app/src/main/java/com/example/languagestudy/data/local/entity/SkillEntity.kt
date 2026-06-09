package com.example.languagestudy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class Subtask(
    val id: String = System.currentTimeMillis().toString(),
    val text: String = "",
    val status: String = "NOT_STARTED" // NOT_STARTED, IN_PROGRESS, PROFICIENT
)

@Entity(tableName = "skills")
data class SkillEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(), // Default to UUID for Firestore alignment
    val name: String,
    val level: String = "Beginner",
    val progress: Int = 0,
    val status: String = "NOT_STARTED",
    val language: String = "",
    val priority: Int = 0,
    val subtasks: List<Subtask> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

class SkillTypeConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromSubtaskList(value: List<Subtask>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toSubtaskList(value: String): List<Subtask> {
        val listType = object : TypeToken<List<Subtask>>() {}.type
        return gson.fromJson(value, listType)
    }
}
