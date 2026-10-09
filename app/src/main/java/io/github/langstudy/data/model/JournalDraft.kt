package io.github.langstudy.data.model

data class JournalDraft(
    val title: String = "",
    val content: String = "",
    val language: String = "",
    val editingId: String = "",
    val mentorVisible: Boolean = false,
    val mentorAccessLevel: String = "view",
    val tags: List<String> = emptyList(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    fun isEmpty(): Boolean = title.isBlank() && content.isBlank()
    fun isNotEmpty(): Boolean = !isEmpty()
}
