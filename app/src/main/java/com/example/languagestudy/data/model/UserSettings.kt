package com.example.languagestudy.data.model

data class UserSettings(
    val learnedLanguages: List<String> = emptyList(),
    val languageLearning: String = "",
    val shareCode: String = "",
    val isPublic: Boolean = false
)
