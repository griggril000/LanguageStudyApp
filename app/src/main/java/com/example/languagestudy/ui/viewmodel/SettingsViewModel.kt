package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.UserSettings
import com.example.languagestudy.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository, private val userId: String) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = repository.getUserSettings(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages.asStateFlow()

    init {
        loadAvailableLanguages()
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            _availableLanguages.value = repository.getAvailableLanguages()
        }
    }

    fun toggleLanguage(language: String) {
        val currentSettings = userSettings.value
        val isSelected = currentSettings.learnedLanguages.contains(language)
        
        // Prevent unselecting if it's the only one
        if (isSelected && currentSettings.learnedLanguages.size <= 1) {
            return
        }

        val newList = if (isSelected) {
            currentSettings.learnedLanguages - language
        } else {
            currentSettings.learnedLanguages + language
        }
        
        val updates = mutableMapOf<String, Any>("learnedLanguages" to newList)
        
        // If exactly one language selected, set it as current (matching JS logic)
        if (newList.size == 1) {
            updates["languageLearning"] = newList[0]
        } else if (newList.isEmpty() || !newList.contains(currentSettings.languageLearning)) {
            updates["languageLearning"] = if (newList.isNotEmpty()) newList[0] else ""
        }

        viewModelScope.launch {
            repository.updateUserSettings(userId, updates)
        }
    }

    fun setCurrentLanguage(language: String) {
        viewModelScope.launch {
            repository.updateUserSettings(userId, mapOf("languageLearning" to language))
        }
    }

    fun togglePublic(isPublic: Boolean) {
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>("isPublic" to isPublic)
            if (isPublic && userSettings.value.shareCode.isBlank()) {
                updates["shareCode"] = generateRandomCode()
            }
            repository.updateUserSettings(userId, updates)
        }
    }

    fun regenerateShareCode() {
        viewModelScope.launch {
            repository.updateUserSettings(userId, mapOf("shareCode" to generateRandomCode()))
        }
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5)
            .map { chars.random() }
            .joinToString("")
    }
}

class SettingsViewModelFactory(private val repository: SettingsRepository, private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
