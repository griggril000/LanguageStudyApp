package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.VocabEntity
import com.example.languagestudy.data.repository.VocabRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VocabViewModel(private val repository: VocabRepository) : ViewModel() {
    val allVocab: StateFlow<List<VocabEntity>> = repository.allVocab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addVocab(word: String, translation: String, category: String) {
        viewModelScope.launch {
            repository.insert(VocabEntity(word = word, translation = translation, category = category))
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
