package com.example.languagestudy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.languagestudy.data.local.entity.SkillEntity
import com.example.languagestudy.data.local.entity.Subtask
import com.example.languagestudy.data.repository.SettingsRepository
import com.example.languagestudy.data.repository.SkillRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SkillViewModel(
    private val repository: SkillRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private var userId: String? = null

    val allSkills: StateFlow<List<SkillEntity>> = repository.allSkills
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedLanguage = MutableStateFlow<String?>(null)
    val selectedLanguage: StateFlow<String?> = _selectedLanguage.asStateFlow()

    private val _learnedLanguages = MutableStateFlow<List<String>>(emptyList())
    val learnedLanguages: StateFlow<List<String>> = _learnedLanguages.asStateFlow()

    val filteredSkills: StateFlow<List<SkillEntity>> = combine(allSkills, _searchQuery, _selectedLanguage) { skills, query, lang ->
        skills.filter { skill ->
            (query.isBlank() || skill.name.contains(query, ignoreCase = true) || 
             skill.level.contains(query, ignoreCase = true) ||
             skill.status.contains(query, ignoreCase = true) ||
             skill.language.contains(query, ignoreCase = true)) &&
            (lang == null || lang.isBlank() || skill.language == lang)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLanguages: StateFlow<List<String>> = allSkills.map { skills ->
        skills.map { it.language }.filter { it.isNotBlank() }.distinct().sorted()
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
                if (_selectedLanguage.value == null) {
                    _selectedLanguage.value = settings.languageLearning
                }
            }
        }
    }

    fun addSkill(input: String, language: String = "") {
        if (input.isBlank()) {
            viewModelScope.launch { _error.emit("Skill name cannot be empty") }
            return
        }
        
        val skillNames = input.split("\n").filter { it.trim().isNotBlank() }
        val currentMaxPriority = allSkills.value.maxOfOrNull { it.priority } ?: -1
        
        viewModelScope.launch {
            try {
                skillNames.forEachIndexed { index, name ->
                    repository.insert(SkillEntity(
                        name = name.trim(), 
                        language = language,
                        priority = currentMaxPriority + 1 + index
                    ), userId)
                }
            } catch (e: Exception) {
                _error.emit("Failed to add skill: ${e.message}")
            }
        }
    }

    fun moveSkill(fromIndex: Int, toIndex: Int) {
        val list = filteredSkills.value.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        updateSkillOrder(list)
    }

    fun updateSkillOrder(newList: List<SkillEntity>) {
        viewModelScope.launch {
            newList.forEachIndexed { index, skill ->
                if (skill.priority != index) {
                    repository.update(skill.copy(priority = index), userId)
                }
            }
        }
    }

    fun updateSkill(skill: SkillEntity) {
        val calculatedProgress = if (skill.subtasks.isEmpty()) {
            if (skill.status == "PROFICIENT") 100 else if (skill.status == "IN_PROGRESS") 50 else 0
        } else {
            val proficientCount = skill.subtasks.count { it.status == "PROFICIENT" }
            (proficientCount * 100) / skill.subtasks.size
        }
        
        viewModelScope.launch {
            repository.update(skill.copy(
                progress = calculatedProgress,
                lastUpdated = System.currentTimeMillis()
            ), userId)
        }
    }

    fun deleteSkill(skill: SkillEntity) {
        viewModelScope.launch {
            repository.delete(skill, userId)
        }
    }

    fun cycleSkillStatus(skill: SkillEntity) {
        val nextStatus = when (skill.status) {
            "NOT_STARTED" -> "IN_PROGRESS"
            "IN_PROGRESS" -> "PROFICIENT"
            "PROFICIENT" -> "NOT_STARTED"
            else -> "NOT_STARTED"
        }
        updateSkill(skill.copy(status = nextStatus))
    }

    fun addSubtask(skill: SkillEntity, text: String) {
        if (text.isBlank()) return
        val newSubtasks = skill.subtasks + Subtask(text = text.trim())
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun updateSubtaskStatus(skill: SkillEntity, subtaskId: String) {
        val newSubtasks = skill.subtasks.map { 
            if (it.id == subtaskId) {
                val nextStatus = when (it.status) {
                    "NOT_STARTED" -> "IN_PROGRESS"
                    "IN_PROGRESS" -> "PROFICIENT"
                    "PROFICIENT" -> "NOT_STARTED"
                    else -> "NOT_STARTED"
                }
                it.copy(status = nextStatus)
            } else it
        }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun deleteSubtask(skill: SkillEntity, subtaskId: String) {
        val newSubtasks = skill.subtasks.filter { it.id != subtaskId }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun updateSubtaskText(skill: SkillEntity, subtaskId: String, newText: String) {
        if (newText.isBlank()) return
        val newSubtasks = skill.subtasks.map { 
            if (it.id == subtaskId) it.copy(text = newText.trim()) else it
        }
        updateSkill(skill.copy(subtasks = newSubtasks))
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedLanguage(language: String?) {
        _selectedLanguage.value = language
    }
}

class SkillViewModelFactory(
    private val repository: SkillRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SkillViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SkillViewModel(repository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
