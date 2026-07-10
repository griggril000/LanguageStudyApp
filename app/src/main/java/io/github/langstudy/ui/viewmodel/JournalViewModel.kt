package io.github.langstudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.langstudy.data.local.entity.JournalEntryEntity
import io.github.langstudy.data.repository.JournalRepository
import io.github.langstudy.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class JournalViewModel(
    private val repository: JournalRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var userId: String? = null
    private val _isMentorMode = MutableStateFlow(false)

    val allEntries: StateFlow<List<JournalEntryEntity>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _learnedLanguages = MutableStateFlow<List<String>>(emptyList())
    val learnedLanguages: StateFlow<List<String>> = _learnedLanguages.asStateFlow()

    private val _currentLanguage = MutableStateFlow("")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val filteredEntries: StateFlow<List<JournalEntryEntity>> =
        combine(allEntries, _searchQuery, _isMentorMode) { entries, query, mentorMode ->
            val baseEntries = if (mentorMode) {
                entries.filter { it.mentorVisible }
            } else {
                entries
            }
            if (query.isBlank()) baseEntries
            else baseEntries.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.content.contains(query, ignoreCase = true)
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun initUserId(id: String, isMentorMode: Boolean = false) {
        _isMentorMode.value = isMentorMode
        if (userId == id) return
        userId = id
        viewModelScope.launch {
            repository.startSync(id)
                .catch { e -> _error.emit("Sync failed: ${e.message}") }
                .collect()
        }
        viewModelScope.launch {
            settingsRepository.getUserSettings(id).collect { settings ->
                _learnedLanguages.value = settings.learnedLanguages
                _currentLanguage.value = settings.languageLearning
            }
        }
    }

    fun saveEntry(
        id: String? = null,
        title: String,
        content: String,
        language: String = "",
        mentorVisible: Boolean = false,
        mentorAccessLevel: String = "view",
        originalTimestamp: Long? = null
    ) {
        if (title.isBlank() || content.isBlank()) {
            viewModelScope.launch { _error.emit("Title and content cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val entry = if (id != null) {
                    JournalEntryEntity(
                        id = id,
                        title = title.trim(),
                        content = content.trim(),
                        language = language,
                        timestamp = originalTimestamp ?: now,
                        dateModified = now,
                        mentorVisible = mentorVisible,
                        mentorAccessLevel = mentorAccessLevel
                    )
                } else {
                    JournalEntryEntity(
                        title = title.trim(),
                        content = content.trim(),
                        language = language,
                        timestamp = now,
                        dateModified = now,
                        mentorVisible = mentorVisible,
                        mentorAccessLevel = mentorAccessLevel
                    )
                }
                repository.insert(entry, userId)
            } catch (e: Exception) {
                _error.emit("Failed to save entry: ${e.message}")
            }
        }
    }

    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            try {
                repository.delete(entry, userId)
            } catch (e: Exception) {
                _error.emit("Failed to delete entry: ${e.message}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCurrentLanguage(language: String) {
        _currentLanguage.value = language
    }
}

class JournalViewModelFactory(
    private val repository: JournalRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
