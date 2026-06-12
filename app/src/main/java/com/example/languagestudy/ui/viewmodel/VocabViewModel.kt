package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.VocabEntity
import com.example.languagestudy.data.repository.VocabRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VocabViewModel(private val repository: VocabRepository) : ViewModel() {
    private var userId: String? = null

    val allVocab: StateFlow<List<VocabEntity>> = repository.allVocab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val filteredVocab: StateFlow<List<VocabEntity>> = combine(allVocab, _searchQuery) { vocab, query ->
        if (query.isBlank()) vocab
        else vocab.filter { 
            it.word.contains(query, ignoreCase = true) || 
            it.translation.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
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

    fun addVocab(word: String, translation: String, category: String, language: String = "en") {
        if (word.isBlank() || translation.isBlank()) {
            viewModelScope.launch { _error.emit("Word and translation cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insert(VocabEntity(word = word.trim(), translation = translation.trim(), category = category, language = language), userId)
            } catch (e: Exception) {
                _error.emit("Failed to add vocab: ${e.message}")
            }
        }
    }

    fun deleteVocab(vocab: VocabEntity) {
        viewModelScope.launch {
            repository.delete(vocab, userId)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class VocabViewModelFactory(private val repository: VocabRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VocabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VocabViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
