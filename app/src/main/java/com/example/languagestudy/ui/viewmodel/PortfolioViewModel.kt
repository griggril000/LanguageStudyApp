package com.example.languagestudy.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.PortfolioItem
import com.example.languagestudy.data.repository.FirestorePortfolioRepository
import com.example.languagestudy.data.repository.PortfolioRepository
import com.example.languagestudy.utils.UrlUtils
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val userId: String,
    private val repository: PortfolioRepository = FirestorePortfolioRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val items: StateFlow<List<PortfolioItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
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
    }

    fun addItem(title: String, link: String) {
        if (title.isBlank()) {
            viewModelScope.launch { _error.emit("Title cannot be empty") }
            return
        }
        
        val sanitizedUrl = UrlUtils.sanitizeHttpUrl(link)
        if (sanitizedUrl == null) {
            viewModelScope.launch { _error.emit("Please enter a valid HTTP/HTTPS link") }
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val resolvedUrl = UrlUtils.resolveSoundCloudPortfolioLink(sanitizedUrl)
                val type = UrlUtils.getPortfolioType(resolvedUrl) ?: "link"
                val youtubeId = UrlUtils.getYouTubeId(resolvedUrl)
                
                repository.addPortfolioItem(
                    userId, 
                    PortfolioItem(
                        title = title.trim(), 
                        link = resolvedUrl,
                        type = type,
                        videoId = youtubeId
                    )
                )
            } catch (e: Exception) {
                _error.emit("Failed to add item: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deletePortfolioItem(userId, id)
        }
    }

    fun toggleFeatured(item: PortfolioItem) {
        viewModelScope.launch {
            repository.updatePortfolioItem(userId, item.copy(isTop = !item.isTop))
        }
    }
}

class PortfolioViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PortfolioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PortfolioViewModel(userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
