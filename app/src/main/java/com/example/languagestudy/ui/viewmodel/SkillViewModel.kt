package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.repository.SkillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SkillViewModel(private val repository: SkillRepository) : ViewModel() {
    val allSkills: StateFlow<List<SkillEntity>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun addSkill(name: String, level: String) {
        if (name.isBlank()) {
            viewModelScope.launch { _error.emit("Skill name cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insert(SkillEntity(name = name.trim(), level = level))
            } catch (e: Exception) {
                _error.emit("Failed to add skill: ${e.message}")
            }
        }
    }

    fun updateProgress(skill: SkillEntity, progress: Int) {
        viewModelScope.launch {
            repository.update(skill.copy(progress = progress))
        }
    }
}

class SkillViewModelFactory(private val repository: SkillRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SkillViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
