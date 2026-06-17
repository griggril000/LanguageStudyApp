package io.github.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.languagestudy.data.local.entity.VocabEntity
import io.github.languagestudy.data.repository.SettingsRepository
import io.github.languagestudy.data.repository.VocabRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VocabViewModel(
    private val repository: VocabRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var userId: String? = null

    val allVocab: StateFlow<List<VocabEntity>> = repository.allVocab
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentLanguage = MutableStateFlow("")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _learnedLanguages = MutableStateFlow<List<String>>(emptyList())
    val learnedLanguages: StateFlow<List<String>> = _learnedLanguages.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val categories: StateFlow<List<String>> = combine(repository.allCategories, allVocab) { repoCats, vocabList ->
        val fromRepo = repoCats.map { it.name }
        val fromVocab = vocabList.map { it.category }
        val names = (fromRepo + fromVocab).distinct().filter { it != "General" }.sorted().toMutableList()
        names.add(0, "General")
        names
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("General"))

    val filteredVocab: StateFlow<List<VocabEntity>> = combine(allVocab, _searchQuery, _currentLanguage, _selectedCategory) { vocab, query, lang, cat ->
        vocab.filter { 
            (query.isBlank() || 
             it.word.contains(query, ignoreCase = true) || 
             it.translation.contains(query, ignoreCase = true) ||
             it.category.contains(query, ignoreCase = true)) &&
            (lang.isBlank() || it.language == lang) &&
            (cat == null || it.category == cat)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun initUserId(id: String) {
        if (userId == id) return
        userId = id
        viewModelScope.launch {
            repository.startSync(id).collect()
        }
        viewModelScope.launch {
            settingsRepository.getUserSettings(id).collect { settings ->
                _learnedLanguages.value = settings.learnedLanguages
                _currentLanguage.value = settings.languageLearning
            }
        }
    }

    fun addVocab(word: String, translation: String, category: String, language: String = "en") {
        if (word.isBlank()) {
            viewModelScope.launch { _error.emit("Word cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insert(VocabEntity(word = word.trim(), translation = translation.trim(), category = category, language = language), userId)
            } catch (e: Exception) {
                _error.emit("Failed to add vocab: ${e.message}")
            }
        }
    }

    fun deleteVocab(vocab: VocabEntity) {
        viewModelScope.launch {
            try {
                repository.delete(vocab, userId)
            } catch (e: Exception) {
                _error.emit("Failed to delete vocab: ${e.message}")
            }
        }
    }

    fun updateVocab(vocab: VocabEntity) {
        viewModelScope.launch {
            try {
                repository.update(vocab, userId)
            } catch (e: Exception) {
                _error.emit("Failed to update vocab: ${e.message}")
            }
        }
    }

    fun cycleVocabStatus(vocab: VocabEntity) {
        val nextStatus = when (vocab.status) {
            "NOT_STARTED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "PROFICIENT"
            "PROFICIENT" -> "NOT_STARTED"
            else -> "NOT_STARTED"
        }
        updateVocab(vocab.copy(status = nextStatus))
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCurrentLanguage(language: String) {
        _currentLanguage.value = language
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.addCategory(name.trim(), userId)
            } catch (e: Exception) {
                _error.emit("Failed to add category: ${e.message}")
            }
        }
    }

    fun deleteCategory(name: String) {
        if (name == "General") return
        viewModelScope.launch {
            try {
                repository.deleteCategory(name, userId)
                if (_selectedCategory.value == name) {
                    _selectedCategory.value = null
                }
            } catch (e: Exception) {
                _error.emit("Failed to delete category: ${e.message}")
            }
        }
    }

    fun seedSampleData() {
        val samples = listOf(
            VocabEntity(word = "Hola", translation = "Hello", category = "Greeting", language = "Spanish", status = "PROFICIENT"),
            VocabEntity(word = "Gracias", translation = "Thank you", category = "Greeting", language = "Spanish", status = "IN_PROGRESS"),
            VocabEntity(word = "Bonjour", translation = "Hello", category = "Greeting", language = "French", status = "NOT_STARTED"),
            VocabEntity(word = "S'il vous plaît", translation = "Please", category = "Politeness", language = "French", status = "PROFICIENT"),
            VocabEntity(word = "Konnichiwa", translation = "", category = "Greeting", language = "Japanese", status = "IN_PROGRESS"),
            VocabEntity(word = "Arigato", translation = "Thank you", category = "Greeting", language = "Japanese", status = "NOT_STARTED")
        )
        viewModelScope.launch {
            samples.forEach { repository.insert(it, userId) }
        }
    }
}

class VocabViewModelFactory(
    private val repository: VocabRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VocabViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VocabViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
