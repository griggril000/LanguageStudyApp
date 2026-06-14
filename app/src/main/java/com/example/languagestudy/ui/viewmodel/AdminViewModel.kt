package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.LanguageResource
import com.example.languagestudy.data.repository.AdminRepository
import com.example.languagestudy.data.repository.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _languages = MutableStateFlow<List<String>>(emptyList())
    val languages: StateFlow<List<String>> = _languages.asStateFlow()

    private val _selectedLanguage = MutableStateFlow("")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _links = MutableStateFlow<List<LanguageResource>>(emptyList())
    val links: StateFlow<List<LanguageResource>> = _links.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    private var userId: String = ""

    init {
        loadLanguages()
    }

    fun initUserId(id: String) {
        userId = id
    }

    private fun loadLanguages() {
        viewModelScope.launch {
            try {
                _languages.value = settingsRepository.getAvailableLanguages()
            } catch (e: Exception) {
                _error.emit("Failed to load languages: ${e.message}")
            }
        }
    }

    fun selectLanguage(language: String) {
        _selectedLanguage.value = language
        if (language.isNotBlank()) {
            loadLinks(language)
        } else {
            _links.value = emptyList()
        }
    }

    private fun loadLinks(language: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _links.value = settingsRepository.getLanguageResources(language)
            } catch (e: Exception) {
                _error.emit("Failed to load links: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addLanguage(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                adminRepository.addLanguage(name.trim(), userId)
                loadLanguages()
                selectLanguage(name.trim())
                _message.emit("Language added")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to add language")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLanguage() {
        val language = _selectedLanguage.value
        if (language.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                adminRepository.deleteLanguage(language)
                loadLanguages()
                selectLanguage("")
                _message.emit("Language deleted")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to delete language")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return url.startsWith("http://") || url.startsWith("https://")
    }

    fun addLink(name: String, url: String) {
        val language = _selectedLanguage.value
        if (language.isBlank() || url.isBlank()) return
        if (!isValidUrl(url.trim())) {
            viewModelScope.launch { _error.emit("Invalid URL. Must start with http:// or https://") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentLinks = _links.value.toMutableList()
                currentLinks.add(LanguageResource(name.trim(), url.trim()))
                adminRepository.saveLanguageLinks(language, currentLinks, userId)
                _links.value = currentLinks
                _message.emit("Link added")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to add link")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateLink(index: Int, name: String, url: String) {
        val language = _selectedLanguage.value
        if (language.isBlank() || url.isBlank()) return
        if (!isValidUrl(url.trim())) {
            viewModelScope.launch { _error.emit("Invalid URL. Must start with http:// or https://") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentLinks = _links.value.toMutableList()
                if (index in currentLinks.indices) {
                    currentLinks[index] = LanguageResource(name.trim(), url.trim())
                    adminRepository.saveLanguageLinks(language, currentLinks, userId)
                    _links.value = currentLinks
                    _message.emit("Link updated")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to update link")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteLink(index: Int) {
        val language = _selectedLanguage.value
        if (language.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentLinks = _links.value.toMutableList()
                if (index in currentLinks.indices) {
                    currentLinks.removeAt(index)
                    adminRepository.saveLanguageLinks(language, currentLinks, userId)
                    _links.value = currentLinks
                    _message.emit("Link deleted")
                }
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to delete link")
            } finally {
                _isLoading.value = false
            }
        }
    }
}

class AdminViewModelFactory(
    private val adminRepository: AdminRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AdminViewModel(adminRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
