package com.example.languagestudy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.PortfolioItem
import com.example.languagestudy.data.repository.FirestorePortfolioRepository
import com.example.languagestudy.data.repository.PortfolioRepository
import com.example.languagestudy.data.repository.SettingsRepository
import com.example.languagestudy.utils.UrlUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val userId: String,
    private val repository: PortfolioRepository = FirestorePortfolioRepository(),
    private val settingsRepository: SettingsRepository = SettingsRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val items: StateFlow<List<PortfolioItem>> = _items.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentLanguage = MutableStateFlow("")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    val filteredItems: StateFlow<List<PortfolioItem>> = combine(_items, _searchQuery, _currentLanguage) { items, query, lang ->
        items.filter { 
            (query.isBlank() || it.title.contains(query, ignoreCase = true) || it.link.contains(query, ignoreCase = true)) &&
            (lang.isBlank() || it.language == lang)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    private val _addSuccess = MutableSharedFlow<Unit>()
    val addSuccess: SharedFlow<Unit> = _addSuccess.asSharedFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        if (userId.isBlank()) {
            Log.d("PortfolioVM", "Skipping loadPortfolio for blank userId")
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPortfolioItems(userId, limit = 50)
                .onEach { 
                    _items.value = it
                    _isLoading.value = false
                }
                .catch { e -> 
                    Log.e("PortfolioVM", "Error loading portfolio for $userId", e)
                    _isLoading.value = false
                }
                .collect()
        }
        viewModelScope.launch {
            settingsRepository.getUserSettings(userId).collect { settings ->
                _currentLanguage.value = settings.languageLearning
            }
        }
    }

    fun addItem(title: String, link: String) {
        if (title.isBlank()) {
            viewModelScope.launch { _error.emit("Title cannot be empty") }
            return
        }
        
        val sanitizedUrl = UrlUtils.sanitizeHttpUrl(link)
        if (sanitizedUrl == null) {
            viewModelScope.launch { _error.emit("Please enter a valid YouTube or SoundCloud link") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolvedUrl = UrlUtils.resolveCanonicalUrl(sanitizedUrl)
                val type = UrlUtils.getPortfolioType(resolvedUrl)
                
                if (type == null) {
                    _error.emit("Only YouTube and SoundCloud links are supported")
                    return@launch
                }

                val youtubeId = UrlUtils.getYouTubeId(resolvedUrl)
                
                repository.addPortfolioItem(
                    userId, 
                    PortfolioItem(
                        title = title.trim(), 
                        link = resolvedUrl,
                        type = type,
                        videoId = youtubeId,
                        language = _currentLanguage.value
                    )
                )
                _addSuccess.emit(Unit)
            } catch (e: Exception) {
                _error.emit("Failed to add item: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteItem(id: String) {
        if (id.isBlank()) {
            Log.e("PortfolioVM", "Cannot delete item with empty ID")
            return
        }
        viewModelScope.launch {
            try {
                repository.deletePortfolioItem(userId, id)
            } catch (e: Exception) {
                Log.e("PortfolioVM", "Error deleting item $id", e)
                _error.emit("Failed to delete item")
            }
        }
    }

    fun toggleFeatured(item: PortfolioItem) {
        if (item.id.isBlank()) {
            Log.e("PortfolioVM", "Cannot toggle featured for item with empty ID")
            return
        }
        viewModelScope.launch {
            if (!item.isTop) {
                // If we are trying to feature an item, check the limit
                val featuredCount = _items.value.count { it.isTop }
                if (featuredCount >= 3) {
                    _error.emit("You can only have up to 3 featured items")
                    return@launch
                }
            }
            try {
                repository.updatePortfolioItem(userId, item.copy(isTop = !item.isTop))
            } catch (e: Exception) {
                Log.e("PortfolioVM", "Error toggling featured for item ${item.id}", e)
                _error.emit("Failed to update item")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
}

class PortfolioViewModelFactory(private val userId: String, private val settingsRepository: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(userId, settingsRepository = settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
