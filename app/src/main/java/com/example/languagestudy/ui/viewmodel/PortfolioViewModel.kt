package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.model.PortfolioItem
import com.example.languagestudy.data.repository.FirestorePortfolioRepository
import com.example.languagestudy.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PortfolioViewModel(
    private val repository: PortfolioRepository = FirestorePortfolioRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<PortfolioItem>>(emptyList())
    val items: StateFlow<List<PortfolioItem>> = _items.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPortfolio()
    }

    fun loadPortfolio() {
        viewModelScope.launch {
            _isLoading.value = true
            repository.getPortfolioItems(limit = 50)
                .onEach { _items.value = it }
                .onCompletion { _isLoading.value = false }
                .catch { /* handle error */ }
                .collect()
        }
    }

    fun addItem(title: String, link: String) {
        viewModelScope.launch {
            repository.addPortfolioItem(PortfolioItem(title = title, link = link))
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deletePortfolioItem(id)
        }
    }

    fun toggleFeatured(item: PortfolioItem) {
        viewModelScope.launch {
            repository.updatePortfolioItem(item.copy(isFeatured = !item.isFeatured))
        }
    }
}
