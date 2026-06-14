package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.LanguageResource
import com.example.languagestudy.data.model.UserSettings
import com.example.languagestudy.data.repository.MentorRepository
import com.example.languagestudy.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository,
    private val mentorRepository: MentorRepository,
    private val userId: String
) : ViewModel() {

    val userSettings: StateFlow<UserSettings> = repository.getUserSettings(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    private val _mentorCode = MutableStateFlow<String?>(null)
    val mentorCode: StateFlow<String?> = _mentorCode.asStateFlow()

    private val _availableLanguages = MutableStateFlow<List<String>>(emptyList())
    val availableLanguages: StateFlow<List<String>> = _availableLanguages.asStateFlow()

    private val _resources = MutableStateFlow<List<LanguageResource>>(emptyList())
    val resources: StateFlow<List<LanguageResource>> = _resources.asStateFlow()

    private val _resourceLanguage = MutableStateFlow("")
    val resourceLanguage: StateFlow<String> = _resourceLanguage.asStateFlow()

    init {
        loadAvailableLanguages()
        loadMentorCode()
        
        // Load resources for the initial primary language when settings load
        viewModelScope.launch {
            userSettings.map { it.languageLearning }.distinctUntilChanged().collect { lang ->
                if (lang.isNotBlank() && _resourceLanguage.value.isBlank()) {
                    setResourceLanguage(lang)
                }
            }
        }
    }

    private fun loadMentorCode() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _mentorCode.value = mentorRepository.getMentorCodeIdForUser(userId)
        }
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            _availableLanguages.value = repository.getAvailableLanguages()
        }
    }

    fun setResourceLanguage(language: String) {
        _resourceLanguage.value = language
        viewModelScope.launch {
            _resources.value = repository.getLanguageResources(language)
        }
    }

    fun toggleMentorCode(enabled: Boolean) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val updates = mutableMapOf<String, Any>("mentorCodeEnabled" to enabled)
            if (enabled && _mentorCode.value == null) {
                _mentorCode.value = mentorRepository.generateUniqueMentorCode(userId)
            }
            
            repository.updateUserSettings(userId, updates)
            
            _mentorCode.value?.let { code ->
                mentorRepository.updateMentorCodeStatus(code, enabled)
            }
        }
    }

    fun regenerateMentorCode() {
        if (userId.isBlank()) return
        viewModelScope.launch {
            _mentorCode.value?.let { oldCode ->
                mentorRepository.deleteMentorCode(oldCode)
            }
            val newCode = mentorRepository.generateUniqueMentorCode(userId)
            _mentorCode.value = newCode
        }
    }

    fun setMentorAccessLevel(level: String) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.updateUserSettings(userId, mapOf("mentorAccessLevel" to level))
        }
    }

    fun toggleLanguage(language: String) {
        if (userId.isBlank()) return
        val currentSettings = userSettings.value
        val isSelected = currentSettings.learnedLanguages.contains(language)
        
        if (isSelected && currentSettings.learnedLanguages.size <= 1) return

        val newList = if (isSelected) {
            currentSettings.learnedLanguages - language
        } else {
            currentSettings.learnedLanguages + language
        }
        
        val updates = mutableMapOf<String, Any>("learnedLanguages" to newList)
        
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
        if (userId.isBlank()) return
        viewModelScope.launch {
            repository.updateUserSettings(userId, mapOf("languageLearning" to language))
        }
    }

    private fun generateRandomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..5).map { chars.random() }.joinToString("")
    }
}

class SettingsViewModelFactory(
    private val repository: SettingsRepository,
    private val mentorRepository: MentorRepository,
    private val userId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repository, mentorRepository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
