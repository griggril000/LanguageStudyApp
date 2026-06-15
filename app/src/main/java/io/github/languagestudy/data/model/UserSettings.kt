package io.github.languagestudy.data.model

data class UserSettings(
    val learnedLanguages: List<String> = emptyList(),
    val languageLearning: String = "",
    val shareCode: String = "",
    val isPublic: Boolean = false,
    val mentorCodeEnabled: Boolean = false,
    val mentorAccessLevel: String = "view",
    val mentorQuickReviewEnabled: Boolean = false
)
