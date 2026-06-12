package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.JournalEntryEntity
import com.example.languagestudy.data.repository.JournalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JournalViewModel(private val repository: JournalRepository) : ViewModel() {
    private var userId: String? = null

    val allEntries: StateFlow<List<JournalEntryEntity>> = repository.allEntries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredEntries: StateFlow<List<JournalEntryEntity>> = combine(allEntries, _searchQuery) { entries, query ->
        if (query.isBlank()) entries
        else entries.filter { 
            it.title.contains(query, ignoreCase = true) || 
            it.content.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun initUserId(id: String) {
        if (userId == id) return
        userId = id
        viewModelScope.launch {
            repository.startSync(id).collect()
        }
    }

    fun addEntry(title: String, content: String) {
        if (title.isBlank() || content.isBlank()) {
            viewModelScope.launch { _error.emit("Title and content cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insert(JournalEntryEntity(title = title.trim(), content = content.trim()), userId)
            } catch (e: Exception) {
                _error.emit("Failed to save entry: ${e.message}")
            }
        }
    }

    fun deleteEntry(entry: JournalEntryEntity) {
        viewModelScope.launch {
            repository.delete(entry, userId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class JournalViewModelFactory(private val repository: JournalRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JournalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JournalViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
