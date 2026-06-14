package io.github.languagestudy.data.model

data class JournalDraft(
    val title: String = "",
    val content: String = "",
    val language: String = "",
    val editingId: String = "",
    val mentorVisible: Boolean = false,
    val mentorAccessLevel: String = "view",
    val updatedAtMs: Long = System.currentTimeMillis()
)
