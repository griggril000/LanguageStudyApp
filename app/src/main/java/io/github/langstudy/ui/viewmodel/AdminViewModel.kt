package io.github.langstudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.langstudy.data.model.LanguageResource
import io.github.langstudy.data.repository.AdminRepository
import io.github.langstudy.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
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
                settingsRepository.getAvailableLanguages().collect { languages ->
                    _languages.value = languages
                }
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
                settingsRepository.getLanguageResources(language).collect {
                    _links.value = it
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _error.emit("Failed to load links: ${e.message}")
                _isLoading.value = false
            }
        }
    }

    fun addLanguage(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return
        if (trimmedName.contains("/")) {
            viewModelScope.launch { _error.emit("Language name cannot contain '/'") }
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                adminRepository.addLanguage(trimmedName, userId)
                selectLanguage(trimmedName)
                _message.emit("✓ Language added: $trimmedName")
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
                selectLanguage("")
                _message.emit("Language deleted")
            } catch (e: Exception) {
                _error.emit(e.message ?: "Failed to delete language")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return ""
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed
        }
        return "https://$trimmed"
    }

    private fun isValidUrl(url: String): Boolean {
        val formatted = formatUrl(url)
        return (formatted.startsWith("http://") || formatted.startsWith("https://")) &&
                formatted.contains(".") && !formatted.contains(" ")
    }

    fun addLink(name: String, url: String) {
        val language = _selectedLanguage.value
        if (language.isBlank()) return

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            viewModelScope.launch { _error.emit("Resource name is required") }
            return
        }

        if (url.isBlank()) {
            viewModelScope.launch { _error.emit("URL is required") }
            return
        }

        if (!isValidUrl(url)) {
            viewModelScope.launch { _error.emit("Invalid URL") }
            return
        }

        val finalUrl = formatUrl(url)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentLinks = _links.value.toMutableList()
                currentLinks.add(LanguageResource(trimmedName, finalUrl))
                adminRepository.saveLanguageLinks(language, currentLinks, userId)
                // Flow collection will update _links automatically
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
        if (language.isBlank()) return

        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            viewModelScope.launch { _error.emit("Resource name is required") }
            return
        }

        if (url.isBlank()) {
            viewModelScope.launch { _error.emit("URL is required") }
            return
        }

        if (!isValidUrl(url)) {
            viewModelScope.launch { _error.emit("Invalid URL") }
            return
        }

        val finalUrl = formatUrl(url)

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentLinks = _links.value.toMutableList()
                if (index in currentLinks.indices) {
                    currentLinks[index] = LanguageResource(trimmedName, finalUrl)
                    adminRepository.saveLanguageLinks(language, currentLinks, userId)
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
