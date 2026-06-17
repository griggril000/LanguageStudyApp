package io.github.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.languagestudy.data.model.LanguageResource
import io.github.languagestudy.data.model.UserSettings
import io.github.languagestudy.data.repository.MentorRepository
import io.github.languagestudy.data.repository.SettingsRepository
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

    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages = _errorMessages.asSharedFlow()

    private val actionCooldowns = mutableMapOf<String, Long>()
    private val actionHistory = mutableListOf<Long>()
    
    private val DEFAULT_COOLDOWN_MS = 1500L
    private val QUICK_COOLDOWN_MS = 600L
    private val MAX_PER_HOUR = 30
    private val MAX_PER_DAY = 100

    init {
        loadAvailableLanguages()
        
        // Load resources for the initial primary language when settings load
        viewModelScope.launch {
            userSettings.map { it.languageLearning }.distinctUntilChanged().collect { lang ->
                if (lang.isNotBlank() && _resourceLanguage.value.isBlank()) {
                    setResourceLanguage(lang)
                }
            }
        }

        // Sync mentorCode state flow with userSettings shareCode, with a fallback check
        viewModelScope.launch {
            userSettings.map { it.shareCode }.distinctUntilChanged().collect { code ->
                if (code.isBlank()) {
                    // Fallback: check if the user has a code in the mentorCodes collection
                    // that hasn't been synced to their settings doc yet.
                    val existingCode = mentorRepository.getMentorCodeIdForUser(userId)
                    if (existingCode != null) {
                        _mentorCode.value = existingCode
                        // Silently sync it back to settings doc
                        repository.updateUserSettings(userId, mapOf("shareCode" to existingCode))
                    } else {
                        _mentorCode.value = null
                    }
                } else {
                    _mentorCode.value = code
                }
            }
        }
    }

    private fun loadAvailableLanguages() {
        viewModelScope.launch {
            try {
                _availableLanguages.value = repository.getAvailableLanguages()
            } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "Error loading languages", e)
            }
        }
    }

    private fun checkRateLimit(action: String, cooldown: Long = DEFAULT_COOLDOWN_MS): Boolean {
        val now = System.currentTimeMillis()
        
        // 1. Check specific action cooldown (tapping spam)
        val last = actionCooldowns[action] ?: 0L
        if (now - last < cooldown) {
            if (cooldown >= DEFAULT_COOLDOWN_MS) {
                viewModelScope.launch {
                    _errorMessages.emit("Please wait a moment before making more changes.")
                }
            }
            return false
        }

        // 2. Check aggregate limits (per hour/day)
        val oneHourAgo = now - 3600000L
        val oneDayAgo = now - 86400000L
        
        // Clean up old history
        actionHistory.removeAll { it < oneDayAgo }
        
        val hourlyCount = actionHistory.count { it > oneHourAgo }
        if (hourlyCount >= MAX_PER_HOUR) {
            viewModelScope.launch {
                _errorMessages.emit("Hourly limit reached for settings changes. Try again later.")
            }
            return false
        }
        
        if (actionHistory.size >= MAX_PER_DAY) {
            viewModelScope.launch {
                _errorMessages.emit("Daily limit reached for settings changes.")
            }
            return false
        }

        actionCooldowns[action] = now
        actionHistory.add(now)
        return true
    }

    fun setResourceLanguage(language: String) {
        _resourceLanguage.value = language
        viewModelScope.launch {
            try {
                repository.getLanguageResources(language).collect { 
                    _resources.value = it
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsVM", "Error loading resources", e)
            }
        }
    }

    fun toggleMentorCode(enabled: Boolean) {
        if (userId.isBlank() || !checkRateLimit("mentor_code_toggle")) return
        viewModelScope.launch {
            try {
                val updates = mutableMapOf<String, Any>("mentorCodeEnabled" to enabled)
                var currentCode = _mentorCode.value
                
                if (enabled && currentCode == null) {
                    currentCode = mentorRepository.generateUniqueMentorCode(userId)
                    _mentorCode.value = currentCode
                    updates["shareCode"] = currentCode
                }
                
                repository.updateUserSettings(userId, updates)
                
                currentCode?.let { code ->
                    mentorRepository.updateMentorCodeStatus(code, enabled)
                }
            } catch (e: Exception) {
                _errorMessages.emit("Failed to update mentor access: ${e.message}")
            }
        }
    }

    fun regenerateMentorCode() {
        if (userId.isBlank() || !checkRateLimit("mentor_code_regenerate", 3000L)) return
        viewModelScope.launch {
            try {
                _mentorCode.value?.let { oldCode ->
                    mentorRepository.deleteMentorCode(oldCode)
                }
                val newCode = mentorRepository.generateUniqueMentorCode(userId)
                _mentorCode.value = newCode
                repository.updateUserSettings(userId, mapOf("shareCode" to newCode))
            } catch (e: Exception) {
                _errorMessages.emit("Failed to regenerate code: ${e.message}")
            }
        }
    }

    fun setMentorAccessLevel(level: String) {
        if (userId.isBlank() || !checkRateLimit("mentor_access_level", QUICK_COOLDOWN_MS)) return
        viewModelScope.launch {
            try {
                repository.updateUserSettings(userId, mapOf("mentorAccessLevel" to level))
            } catch (e: Exception) {
                _errorMessages.emit("Failed to update access level: ${e.message}")
            }
        }
    }

    suspend fun validateCode(code: String): String? {
        return try {
            mentorRepository.validateMentorCode(code)
        } catch (e: Exception) {
            null
        }
    }

    fun toggleLanguage(language: String) {
        if (userId.isBlank() || !checkRateLimit("toggle_language", QUICK_COOLDOWN_MS)) return
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
            try {
                repository.updateUserSettings(userId, updates)
            } catch (e: Exception) {
                _errorMessages.emit("Failed to update languages: ${e.message}")
            }
        }
    }

    fun setCurrentLanguage(language: String) {
        if (userId.isBlank() || !checkRateLimit("set_current_language", QUICK_COOLDOWN_MS)) return
        viewModelScope.launch {
            try {
                repository.updateUserSettings(userId, mapOf("languageLearning" to language))
            } catch (e: Exception) {
                _errorMessages.emit("Failed to set primary language: ${e.message}")
            }
        }
    }

    fun setHomepageTab(tab: String) {
        if (userId.isBlank() || !checkRateLimit("set_homepage_tab", QUICK_COOLDOWN_MS)) return
        viewModelScope.launch {
            try {
                repository.updateUserSettings(userId, mapOf("homepageTab" to tab))
            } catch (e: Exception) {
                _errorMessages.emit("Failed to set start tab: ${e.message}")
            }
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
