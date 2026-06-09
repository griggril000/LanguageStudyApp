package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.VocabEntity
import com.example.languagestudy.data.repository.VocabRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VocabViewModel(private val repository: VocabRepository) : ViewModel() {
    val allVocab: StateFlow<List<VocabEntity>> = repository.allVocab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun addVocab(word: String, translation: String, category: String, language: String = "en") {
        if (word.isBlank() || translation.isBlank()) {
            viewModelScope.launch { _error.emit("Word and translation cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insert(VocabEntity(word = word.trim(), translation = translation.trim(), category = category, language = language))
            } catch (e: Exception) {
                _error.emit("Failed to add vocab: ${e.message}")
            }
        }
    }

    fun deleteVocab(vocab: VocabEntity) {
        viewModelScope.launch {
            repository.delete(vocab)
        }
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
